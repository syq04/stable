import json
import sys
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest

from main import (
    parse_args,
    load_workflow,
    queue_prompt,
    wait_for_completion,
    download_images,
    save_results,
    main,
)


# ── parse_args ──────────────────────────────────────────────────────

def test_parse_args_positive_only():
    with patch("sys.argv", ["main.py", "a cute cat"]):
        args = parse_args()
    assert args.prompt == "a cute cat"
    assert args.negative is None


def test_parse_args_with_negative():
    with patch("sys.argv", ["main.py", "a cute cat", "-n", "ugly"]):
        args = parse_args()
    assert args.prompt == "a cute cat"
    assert args.negative == "ugly"


def test_parse_args_defaults():
    with patch("sys.argv", ["main.py", "hello"]):
        args = parse_args()
    assert args.server == "http://127.0.0.1:8188"
    assert args.output_dir == "outputs"
    assert args.output_file is None
    assert args.workflow == "Workflow.json"
    assert args.steps == 20
    assert args.cfg == 8.0
    assert args.seed is None
    assert args.width == 512
    assert args.height == 512
    assert args.timeout == 120
    assert args.poll_interval == 2


def test_parse_args_overrides():
    with patch("sys.argv", [
        "main.py", "hello",
        "-n", "bad",
        "-s", "http://localhost:9000",
        "-o", "out",
        "-O", "result.png",
        "-w", "wf.json",
        "--steps", "30",
        "--cfg", "7.5",
        "--seed", "42",
        "--width", "1024",
        "--height", "768",
        "--timeout", "60",
        "--poll-interval", "3",
    ]):
        args = parse_args()
    assert args.prompt == "hello"
    assert args.negative == "bad"
    assert args.server == "http://localhost:9000"
    assert args.output_dir == "out"
    assert args.output_file == "result.png"
    assert args.workflow == "wf.json"
    assert args.steps == 30
    assert args.cfg == 7.5
    assert args.seed == 42
    assert args.width == 1024
    assert args.height == 768
    assert args.timeout == 60
    assert args.poll_interval == 3


def test_parse_args_empty_exits():
    with patch("sys.argv", ["main.py"]), pytest.raises(SystemExit):
        parse_args()


# ── load_workflow ───────────────────────────────────────────────────

def test_load_workflow_ok():
    wf = load_workflow("Workflow.json")
    assert wf["26"]["inputs"]["text"] == ""
    assert wf["22"]["inputs"]["text"] == ""
    assert wf["21"]["inputs"]["ckpt_name"] == "v1-5-pruned.safetensors"


def test_load_workflow_missing():
    with pytest.raises(FileNotFoundError):
        load_workflow("nonexistent.json")


# ── save_results ────────────────────────────────────────────────────

def test_save_results(tmp_path, monkeypatch):
    monkeypatch.chdir(tmp_path)

    save_results("a cat", "dog", "pid-123", [("out.png", b"fake_png_data")], output_dir="outputs")

    out_dirs = list(Path("outputs").iterdir())
    assert len(out_dirs) == 1
    folder = out_dirs[0]

    prompts = json.loads((folder / "prompts.json").read_text(encoding="utf-8"))
    assert prompts["positive"] == "a cat"
    assert prompts["negative"] == "dog"
    assert prompts["prompt_id"] == "pid-123"

    assert (folder / "out.png").read_bytes() == b"fake_png_data"


def test_save_results_multiple_images(tmp_path, monkeypatch):
    monkeypatch.chdir(tmp_path)

    images = [("img1.png", b"data1"), ("img2.png", b"data2")]
    save_results("p", "n", "pid-456", images, output_dir="outputs")

    folder = list(Path("outputs").iterdir())[0]
    assert (folder / "img1.png").read_bytes() == b"data1"
    assert (folder / "img2.png").read_bytes() == b"data2"


def test_save_results_with_output_file(tmp_path, monkeypatch):
    monkeypatch.chdir(tmp_path)

    save_results("p", "n", "pid", [("original.png", b"image_data")], output_file="result.png")

    assert Path("result.png").read_bytes() == b"image_data"
    assert not Path("outputs").exists()


def test_save_results_with_output_file_uses_first_image(tmp_path, monkeypatch):
    monkeypatch.chdir(tmp_path)

    save_results("p", "n", "pid", [("a.png", b"first"), ("b.png", b"second")], output_file="out.png")

    assert Path("out.png").read_bytes() == b"first"


def test_save_results_custom_output_dir(tmp_path, monkeypatch):
    monkeypatch.chdir(tmp_path)

    save_results("p", "n", "pid", [("x.png", b"data")], output_dir="my_outs")

    folder = list(Path("my_outs").iterdir())[0]
    assert (folder / "x.png").read_bytes() == b"data"


# ── queue_prompt ────────────────────────────────────────────────────

def test_queue_prompt():
    mock_resp = MagicMock()
    mock_resp.json.return_value = {"prompt_id": "abc-123"}

    with patch("main.requests.post", return_value=mock_resp) as mock_post:
        prompt_id = queue_prompt({"some": "workflow"}, "http://127.0.0.1:8188")

    assert prompt_id == "abc-123"
    mock_post.assert_called_once_with(
        "http://127.0.0.1:8188/prompt",
        json={"prompt": {"some": "workflow"}},
        timeout=10,
    )


# ── wait_for_completion ─────────────────────────────────────────────

def test_wait_for_completion_ok():
    mock_resp = MagicMock()
    mock_resp.json.return_value = {"pid-1": {"outputs": {}}}

    with patch("main.requests.get", return_value=mock_resp), \
         patch("main.time.sleep"):
        result = wait_for_completion("pid-1", "http://127.0.0.1:8188", timeout=120, poll_interval=2)

    assert result == {"outputs": {}}


def test_wait_for_completion_timeout():
    mock_resp = MagicMock()
    mock_resp.json.return_value = {}

    with patch("main.requests.get", return_value=mock_resp), \
         patch("main.time.sleep"), \
         pytest.raises(TimeoutError, match="did not complete"):
        wait_for_completion("pid-timeout", "http://127.0.0.1:8188", timeout=4, poll_interval=2)


# ── download_images ─────────────────────────────────────────────────

def test_download_images():
    history = {
        "outputs": {
            "19": {
                "images": [
                    {"filename": "out_001.png", "type": "output", "subfolder": ""}
                ]
            }
        }
    }
    mock_resp = MagicMock()
    mock_resp.content = b"image_bytes"

    with patch("main.requests.get", return_value=mock_resp) as mock_get:
        images = download_images(history, "http://127.0.0.1:8188")

    assert images == [("out_001.png", b"image_bytes")]
    mock_get.assert_called_once_with(
        "http://127.0.0.1:8188/view",
        params={"filename": "out_001.png", "type": "output", "subfolder": ""},
        timeout=30,
    )


def test_download_images_no_images():
    assert download_images({"outputs": {}}, "http://127.0.0.1:8188") == []


# ── main (integration smoke) ────────────────────────────────────────

@patch("main.save_results")
@patch("main.download_images", return_value=[("img.png", b"data")])
@patch("main.wait_for_completion", return_value={"outputs": {"19": {"images": [{"filename": "img.png"}]}}})
@patch("main.queue_prompt", return_value="pid-999")
@patch("main.load_workflow", return_value={"26": {"inputs": {"text": ""}}, "22": {"inputs": {"text": ""}}, "24": {"inputs": {"seed": 0, "steps": 20, "cfg": 8}}, "25": {"inputs": {"width": 512, "height": 512}}})
def test_main_smoke(load_mock, queue_mock, wait_mock, download_mock, save_mock, tmp_path, monkeypatch):
    monkeypatch.chdir(tmp_path)

    with patch("sys.argv", ["main.py", "a happy dog", "--negative", "sad"]):
        main()

    load_mock.assert_called_once_with("Workflow.json")
    queue_mock.assert_called_once()
    wait_mock.assert_called_once()
    download_mock.assert_called_once()
    save_mock.assert_called_once()


@patch("main.save_results")
@patch("main.download_images", return_value=[("img.png", b"data")])
@patch("main.wait_for_completion", return_value={"outputs": {"19": {"images": [{"filename": "img.png"}]}}})
@patch("main.queue_prompt", return_value="pid-999")
@patch("main.load_workflow", return_value={"26": {"inputs": {"text": ""}}, "22": {"inputs": {"text": ""}}, "24": {"inputs": {"seed": 0, "steps": 20, "cfg": 8}}, "25": {"inputs": {"width": 512, "height": 512}}})
def test_main_smoke_with_seed(load_mock, queue_mock, wait_mock, download_mock, save_mock, tmp_path, monkeypatch):
    monkeypatch.chdir(tmp_path)

    with patch("sys.argv", ["main.py", "hello", "--seed", "123", "--steps", "30"]):
        main()

    workflow = load_mock.call_args[0][0]
    wf = load_mock.return_value
    # load_mock returns the same dict we gave it; main mutates it in place
    assert wf["24"]["inputs"]["seed"] == 123
    assert wf["24"]["inputs"]["steps"] == 30
