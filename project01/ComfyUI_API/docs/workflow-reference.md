# Workflow 节点参考

## 节点总览

`Workflow.json` 定义了一条标准的 txt2img 流水线：

```
CheckpointLoaderSimple (21)
    ├── model ──────────► KSampler (24)
    │                       ├── positive ──► CLIPTextEncode (26)  ← 用户输入
    │                       ├── negative ──► CLIPTextEncode (22)  ← 默认 "cat"
    │                       └── latent ────► EmptyLatentImage (25)
    └── vae ────────────► VAEDecode (19) ──► PreviewImage (23)
```

## 各节点详情

### 节点 21 — CheckpointLoaderSimple（检查点加载器）

```json
{
  "inputs": { "ckpt_name": "v1-5-pruned.safetensors" },
  "class_type": "CheckpointLoaderSimple"
}
```

| 参数 | 说明 |
|------|------|
| `ckpt_name` | 使用的模型文件名。文件需位于 ComfyUI `models/checkpoints/` |

### 节点 26 — CLIPTextEncode（正向提示词编码）

```json
{
  "inputs": { "text": "" },
  "class_type": "CLIPTextEncode"
}
```

| 参数 | 说明 |
|------|------|
| `text` | 初始为空字符串。**始终被 CLI 的 `prompt` 参数覆盖** |

### 节点 22 — CLIPTextEncode（负向提示词编码）

```json
{
  "inputs": { "text": "" },
  "class_type": "CLIPTextEncode"
}
```

| 参数 | 说明 |
|------|------|
| `text` | 初始值为空字符串。**仅当传入 `-n` / `--negative` 时被覆盖** |

### 节点 24 — KSampler（采样器）

```json
{
  "inputs": {
    "seed": 789214052436693,
    "steps": 20,
    "cfg": 8,
    "sampler_name": "euler",
    "scheduler": "simple",
    "denoise": 1
  },
  "class_type": "KSampler"
}
```

| 参数 | CLI 覆盖方式 |
|------|-------------|
| `seed` | `--seed`（不传则保留 Workflow 原值） |
| `steps` | `--steps`（默认 20） |
| `cfg` | `--cfg`（默认 8.0） |
| `sampler_name` | 不可 CLI 覆盖，在 Workflow.json 中手动修改 |
| `scheduler` | 不可 CLI 覆盖，在 Workflow.json 中手动修改 |
| `denoise` | 不可 CLI 覆盖，在 Workflow.json 中手动修改 |

### 节点 25 — EmptyLatentImage（空白潜空间图像）

```json
{
  "inputs": { "width": 512, "height": 512, "batch_size": 1 },
  "class_type": "EmptyLatentImage"
}
```

| 参数 | CLI 覆盖方式 |
|------|-------------|
| `width` | `--width`（默认 512） |
| `height` | `--height`（默认 512） |
| `batch_size` | 不可 CLI 覆盖，在 Workflow.json 中手动修改 |

### 节点 19 — VAEDecode（VAE 解码器）

将潜空间表示解码为像素图像。无需配置。

### 节点 23 — PreviewImage（预览图像）

ComfyUI 内置的图像输出节点。API 模式下生成的图片通过此节点输出。

## 参数覆盖逻辑（按执行顺序）

1. 加载 Workflow.json
2. `workflow["26"]["inputs"]["text"] = args.prompt`  — 始终覆盖
3. 如果 `args.negative` 不为 None：`workflow["22"]["inputs"]["text"] = args.negative`
4. 如果 `args.seed` 不为 None：`workflow["24"]["inputs"]["seed"] = args.seed`
5. `workflow["24"]["inputs"]["steps"] = args.steps` — 始终覆盖
6. `workflow["24"]["inputs"]["cfg"] = args.cfg` — 始终覆盖
7. `workflow["25"]["inputs"]["width"] = args.width` — 始终覆盖
8. `workflow["25"]["inputs"]["height"] = args.height` — 始终覆盖
