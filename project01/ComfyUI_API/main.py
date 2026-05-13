import argparse
import json
import time
from datetime import datetime
from pathlib import Path

import requests


def parse_args():
    parser = argparse.ArgumentParser(description="ComfyUI 提示词生成器")
    parser.add_argument("prompt", help="正向提示词")
    parser.add_argument("-n", "--negative", help="负向提示词")
    parser.add_argument("-s", "--server", default="http://127.0.0.1:8188", help="ComfyUI 服务地址")
    parser.add_argument("-o", "--output-dir", default="outputs", help="输出根目录")
    parser.add_argument("-O", "--output-file", help="直接保存图片到该路径，跳过文件夹和 prompts.json")
    parser.add_argument("-w", "--workflow", default="Workflow.json", help="Workflow JSON 文件路径")
    parser.add_argument("--steps", type=int, default=20, help="采样步数")
    parser.add_argument("--cfg", type=float, default=8.0, help="CFG 尺度")
    parser.add_argument("--seed", type=int, help="随机种子（不传则保留 Workflow 原值）")
    parser.add_argument("--width", type=int, default=512, help="图片宽度")
    parser.add_argument("--height", type=int, default=512, help="图片高度")
    parser.add_argument("--timeout", type=int, default=120, help="最大等待秒数")
    parser.add_argument("--poll-interval", type=int, default=2, help="轮询间隔秒数")
    return parser.parse_args()


def load_workflow(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def queue_prompt(workflow, base_url):
    resp = requests.post(f"{base_url}/prompt", json={"prompt": workflow}, timeout=10)
    resp.raise_for_status()
    return resp.json()["prompt_id"]


def wait_for_completion(prompt_id, base_url, timeout, poll_interval=2):
    for _ in range(timeout // poll_interval):
        time.sleep(poll_interval)
        resp = requests.get(f"{base_url}/history/{prompt_id}", timeout=10)
        resp.raise_for_status()
        history = resp.json()
        if prompt_id in history:
            return history[prompt_id]
    raise TimeoutError(f"Prompt {prompt_id} did not complete within {timeout}s")


def download_images(history_result, base_url):
    images = []
    for node_output in history_result.get("outputs", {}).values():
        for img in node_output.get("images", []):
            resp = requests.get(
                f"{base_url}/view",
                params={"filename": img["filename"], "type": img.get("type", "output"), "subfolder": img.get("subfolder", "")},
                timeout=30,
            )
            resp.raise_for_status()
            images.append((img["filename"], resp.content))
    return images


def save_results(positive, negative, prompt_id, images, output_dir="outputs", output_file=None):
    if output_file:
        path = Path(output_file)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(images[0][1])
        if len(images) > 1:
            print(f"Warning: {len(images)} images generated, only saved first to {path}")
        print(f"Saved to {path}")
        return

    timestamp = datetime.now()
    folder = Path(output_dir) / timestamp.strftime("%Y%m%d_%H%M%S")
    folder.mkdir(parents=True, exist_ok=True)

    prompts = {
        "positive": positive,
        "negative": negative,
        "prompt_id": prompt_id,
        "timestamp": timestamp.isoformat(),
    }
    with open(folder / "prompts.json", "w", encoding="utf-8") as f:
        json.dump(prompts, f, ensure_ascii=False, indent=2)

    for filename, data in images:
        (folder / filename).write_bytes(data)

    print(f"Saved {len(images)} image(s) to {folder}")


def main():
    args = parse_args()

    workflow = load_workflow(args.workflow)

    workflow["26"]["inputs"]["text"] = args.prompt
    if args.negative is not None:
        workflow["22"]["inputs"]["text"] = args.negative
    if args.seed is not None:
        workflow["24"]["inputs"]["seed"] = args.seed
    workflow["24"]["inputs"]["steps"] = args.steps
    workflow["24"]["inputs"]["cfg"] = args.cfg
    workflow["25"]["inputs"]["width"] = args.width
    workflow["25"]["inputs"]["height"] = args.height

    print(f"Queuing prompt: positive={args.prompt!r}, negative={args.negative!r}")
    prompt_id = queue_prompt(workflow, args.server)
    print(f"Prompt queued, id: {prompt_id}")

    result = wait_for_completion(prompt_id, args.server, args.timeout, args.poll_interval)
    images = download_images(result, args.server)
    save_results(args.prompt, args.negative, prompt_id, images, args.output_dir, args.output_file)


if __name__ == "__main__":
    main()
