# 用户手册

## 环境要求

- Python >= 3.14（通过 `uv` 管理）
- ComfyUI 服务运行在 `http://127.0.0.1:8188`
- Workflow 使用的模型文件（如 `v1-5-pruned.safetensors`）已放置在 ComfyUI 的 `models/checkpoints/` 目录

## 安装

```powershell
uv add requests
```

`uv` 是唯一的包管理器。不要使用 `pip`。

## 使用场景

### 场景一：生成图片并保存到时间戳文件夹（默认）

```powershell
uv run python main.py "a cat wearing a hat"
```

输出：

```
outputs/
└── 20260512_143025/
    ├── ComfyUI_00001_.png
    └── prompts.json
```

### 场景二：指定负向提示词

```powershell
uv run python main.py "a cat wearing a hat" -n "ugly, deformed"
```

`-n` / `--negative` 不传时，Workflow 保留负向提示词 `"cat"`。

### 场景三：直接保存到单个文件

```powershell
uv run python main.py "a cat" -O result.png
```

跳过文件夹创建和 `prompts.json`，只保存第一张生成的图片到指定路径。
适用于快速预览或管道集成。如果生成了多张图片，只保存第一张并给出警告。

### 场景四：自定义 Workflow 和服务地址

```powershell
uv run python main.py "a cat" -w my_workflow.json -s http://192.168.1.100:8188
```

### 场景五：覆盖采样参数

```powershell
uv run python main.py "a cat" --steps 30 --cfg 10 --seed 42 --width 1024 --height 768
```

### 场景六：自定义输出目录

```powershell
uv run python main.py "a cat" -o my_outputs
```

时间戳文件夹将在 `my_outputs/` 下创建。

## 完整参数列表

使用 `-h` 查看所有参数：

```powershell
uv run python main.py -h
```

## 输出文件说明

### prompts.json

```json
{
  "positive": "a cat wearing a hat",
  "negative": "ugly",
  "prompt_id": "103f41a0-5468-4fc4-bb3b-5c9d4080dbd3",
  "timestamp": "2026-05-12T14:30:25"
}
```

| 字段 | 说明 |
|------|------|
| `positive` | 本次使用的正向提示词 |
| `negative` | 本次使用的负向提示词（可能为 `null`） |
| `prompt_id` | ComfyUI 返回的任务 ID |
| `timestamp` | 提交时间（ISO 8601） |

## 常见问题

### Q: ComfyUI 服务连接不上

确认：
1. ComfyUI 已启动
2. 服务地址正确（默认 `http://127.0.0.1:8188`）
3. 使用 `-s` 参数指定自定义地址

### Q: 生成超时

默认等待 120 秒。对于复杂的 Workflow 或慢速 GPU，使用 `--timeout` 增加等待时间：

```powershell
uv run python main.py "a cat" --timeout 300
```

### Q: Workflow.json 报错

确认文件格式为有效的 JSON。可以使用 `-w` 指定其他文件路径。

### Q: uv 命令找不到

参考 [uv 官方安装文档](https://docs.astral.sh/uv/#installation)。
