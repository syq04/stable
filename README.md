# Nebula Studio — 文图互转主题设计系统

[文图互转主题设计系统 + ComfyUI API 客户端]

## 项目结构

```
├── project01/                  # Nebula Studio 主项目
│   ├── frontend/               # Vue 3 + Vite + Element Plus 前端
│   ├── backend/                # Spring Boot 3 + Java 17 后端
│   ├── docs/                   # 设计文档（系统思考/需求/架构/设计/原型）
│   ├── build.bat               # 构建脚本
│   ├── start.bat / start.ps1   # 启动脚本
│   └── test.bat / test.ps1     # 测试脚本
├── ComfyUI_API/                # ComfyUI Python CLI 客户端
│   ├── main.py                 # 命令行入口
│   ├── Workflow.json           # ComfyUI 工作流定义
│   └── tests/                  # pytest 测试
├── ComfyUI_windows_portable_nvidia_cu126/  # ComfyUI 本地部署包
└── README.md
```

## Nebula Studio

前后端分离的文图互转主题设计系统。

### 技术栈

| 层 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia + Axios |
| 后端 | Spring Boot 3.2 + Java 17 + MyBatis-Plus + Spring Security + JWT |
| AI | ComfyUI（策略模式：Text2ImageProvider / Image2TextProvider）|
| 数据库 | H2（开发）/ MySQL（生产）|
| 构建 | Maven + npm |

### 快速启动

```bash
# 开发模式
cd project01
build.bat          # 构建全部
start.bat          # 启动前后端服务
```

- 前端: http://localhost:3000
- 后端: http://localhost:8080
- 默认管理员: `admin@nebula.com` / `admin123`

### 数据库

默认使用 H2 文件数据库（profile: `h2`），MySQL 需指定 profile: `mysql`。

## ComfyUI API 客户端

Python 命令行工具，通过 ComfyUI REST API 提交工作流并生成图片。

### 使用

```bash
cd ComfyUI_API
uv run python main.py "a cute cat" -n "ugly"
```

### 参数

| 参数 | 说明 |
|------|------|
| `prompt` | 正向提示词（必需） |
| `-n` | 负向提示词 |
| `-s` | ComfyUI 服务地址（默认 `http://127.0.0.1:8188`）|
| `--steps` / `--cfg` / `--seed` | 采样参数 |
| `-O` | 直接输出到文件 |

## 许可证

MIT
