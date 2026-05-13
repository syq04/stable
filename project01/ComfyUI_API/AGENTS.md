# AGENTS.md

## 项目目标

一个 ComfyUI 测试自动化脚本：通过 `requests` 将 `Workflow.json` 发送到 `http://127.0.0.1:8188/prompt`，填写自定义提示词（正/负向），生成图片并保存到时间戳命名的目录。

## 快速开始

```powershell
uv add requests     # 添加缺失的依赖
uv run python main.py "a cute cat" -n "ugly"       # 保存到时间戳文件夹
uv run python main.py "a cute cat" -O result.png   # 直接保存到单个文件
```

`uv` 是唯一的包管理器 — 请勿使用 `pip`。

## CLI 用法

```
uv run python main.py [选项] <正向提示词>
```

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `prompt` | — | 正向提示词（必需） |
| `-n`, `--negative` | — | 负向提示词，不传则保留 Workflow 原值（当前为空） |
| `-s`, `--server` | `http://127.0.0.1:8188` | ComfyUI 服务地址 |
| `-o`, `--output-dir` | `outputs` | 输出根目录 |
| `-O`, `--output-file` | — | 直接保存图片到该路径，跳过文件夹和 prompts.json |
| `-w`, `--workflow` | `Workflow.json` | Workflow 文件路径 |
| `--steps` | 20 | 采样步数 |
| `--cfg` | 8.0 | CFG 尺度 |
| `--seed` | Workflow 原值 | 随机种子 |
| `--width` / `--height` | 512 / 512 | 图片尺寸 |
| `--timeout` | 120 | 最大等待秒数 |
| `--poll-interval` | 2 | 轮询间隔秒数 |

## Workflow.json 说明

- **节点 22**: CLIPTextEncode — `text: ""`（负向提示词，传入 `--negative` 时覆盖）
- **节点 26**: CLIPTextEncode — `text: ""`（正向提示词，始终用 CLI 参数覆盖）
- **节点 21**: CheckpointLoaderSimple — `ckpt_name: "v1-5-pruned.safetensors"`
- **节点 24**: KSampler — seed、steps 20、cfg 8、euler 采样器

## ComfyUI API 流程

1. `POST /prompt` 携带 `{"prompt": <workflow_dict>}` → 返回 `{"prompt_id": "..."}`
2. 轮询 `GET /history/{prompt_id}`（每 2 秒，最长 120 秒）
3. `GET /view?filename=...&type=output` → 下载生成的图片

## 输出目录

```
outputs/
└── YYYYMMDD_HHMMSS/
    ├── ComfyUI_00001_.png
    └── prompts.json          # 记录正/负向提示词、prompt_id、时间戳
```

## 入口

`main.py` — 包含完整流程：解析参数 → 加载 Workflow → 注入提示词 → 提交 → 轮询 → 下载 → 保存。

## 测试

```powershell
uv add --dev pytest           # 一次性的
uv run pytest tests/ -v       # 运行测试
```

测试在 `tests/test_main.py`，覆盖解析、加载、保存、HTTP 交互和全流程冒烟（外部调用均 mock）。
