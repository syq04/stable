# Nebula Studio v1.2.0 — 文图互转主题设计系统

本地 GPU 推理驱动的 AI 图像生成与分析平台。

[![Version](https://img.shields.io/badge/version-1.2.0-blue)](#)
[![Java](https://img.shields.io/badge/Java-17-orange)](https://adoptium.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.12-brightgreen)](https://spring.io)
[![Vue](https://img.shields.io/badge/Vue-3.4-42b883)](https://vuejs.org)
[![Python](https://img.shields.io/badge/Python-3.10+-3776ab)](https://python.org)

---

## 项目结构

```
├── project01/                       # Nebula Studio 主项目
│   ├── frontend/                    # Vue 3 + Vite + Element Plus 前端 (port 3000)
│   ├── backend/                     # Spring Boot 3.2 + Java 17 后端 (port 8080)
│   ├── inference-service/           # Python FastAPI 推理服务 (port 5000)
│   ├── sd-models/                   # safetensors 模型文件存放目录
│   ├── docs/                        # 设计文档
│   ├── start.bat / stop.bat         # 一键启停
│   ├── build.bat                    # 构建脚本
│   ├── test_full.py                 # 全量回归测试
│   └── AGENTS.md                    # 详细开发文档
├── test_full.py                     # 全量回归测试入口
├── test_report.txt                  # 测试报告（自动生成）
└── README.md
```

## 技术栈

| 层 | 技术 | 端口 |
|------|------|------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia | 3000 |
| 后端 | Spring Boot 3.2 + Java 17 + MyBatis-Plus + JWT | 8080 |
| 文生图推理 | Python FastAPI + diffusers + torch CUDA | 5000 |
| 图生文 | 千问 Qwen3.5-Omni-Plus (DashScope API) | — |
| 数据库 | H2（开发）/ MySQL（生产） |
| 测试 | `test_full.py` 全量回归测试（70 项） |

## 快速启动

```powershell
cd project01
start.bat          # 一键启动全部服务（推理服务 → 后端 → 前端）
stop.bat           # 一键停止全部服务
```

- 前端: http://localhost:3000
- 后端 API: http://localhost:8080
- 推理服务: http://127.0.0.1:5000/health
- 默认管理员: `admin@nebula.com` / `admin123`

> **注意**：推理服务首次启动需加载 SD 1.5 模型到 GPU（7.7 GB），约 2-5 分钟。`start.bat` 会自动删除 H2 数据库锁文件，避免异常退出后无法重启。

## 全量回归测试

```powershell
pip install requests pillow colorama
python F:\Stable\test_full.py
```

覆盖 70 项测试（推理服务白盒/黑盒、后端 API 白盒/黑盒、权限校验、边界条件、输入校验、速率限制、端到端流程），报告自动保存至 `F:\Stable\test_report.txt`。

支持环境变量指定非 localhost 地址：
```powershell
$env:TEST_BACKEND_URL="http://<IP>:8080"
$env:TEST_INFERENCE_URL="http://<IP>:5000"
python F:\Stable\test_full.py
```

## 文生图架构

```
前端 → 后端 API → 推理服务 POST /generate → diffusers + GPU (RTX 4060 8GB) → base64 图片
```

- 模型采用 SD 1.5 safetensors 本地文件，fp16 推理约 3.7GB 显存，512×512 生成约 6 秒
- **多模型支持**：将 `.safetensors` 文件放入 `sd-models/`，前端自动识别并提供模型选择下拉框
- 文生图页面提供全部可调参数（宽度/高度/步数/CFG/采样器/种子），含**实时进度条**（扩散步数/速度/剩余时间）
- 后端参数校验：宽度 64-2048，高度 64-2048，步数 1-150

## 图生文架构

```
前端 → 后端 API → 千问 DashScope API (qwen3.5-omni-plus) → JSON 分析结果
```

- 千问 API Key 在「系统配置」页面管理（管理员登录后：`/admin/settings`）
- 返回结构化 JSON：`{ description, tags, style, prompt }`
- `POST /api/image2text/analyze` — 上传图片进行分析
- 缺文件参数时返回 `400` 错误提示，而非服务器内部错误

## 开发命令

| 操作 | 命令 |
|------|------|
| 启动全部 | `start.bat` |
| 停止全部 | `stop.bat` |
| 构建全部 | `build.bat` |
| 全量回归测试 | `python F:\Stable\test_full.py` |
| 冒烟测试 | `test.bat` 或 `test.ps1` |
| 前端开发 | `npm run dev` (在 `frontend/` 目录) |
| 后端运行 | `mvn spring-boot:run -Dspring-boot.run.profiles=h2` |
| 推理服务 | `python main.py` (在 `inference-service/` 目录) |

## v1.2.0 更新（2026-06-10）

- **文生图准确度评估**：新增「生成并评估」功能，生成后调用千问评估图片与 prompt 匹配度（0-100 分）
- **评估 Bug 修复**：修复评估 prompt 模板中 `%` 未转义导致 `UnknownFormatConversionException` 的问题
- **评估 UI 优化**：评估面板移至图片右侧，移除灰色进度条，仅保留彩色准确度百分比文字
- **仓库清理**：删除 17 个无用文件（application-dev.yml、LoRA 原型、旧脚本、运行时日志等）

## v1.1.0 更新（2026-05-30）

- **全量回归测试**：新增 70 项黑盒+白盒回归测试脚本
- **启动脚本修复**：`start.bat` JAR 路径 Bug 修复 + H2 锁文件自动清理；`stop.bat` 路径变量修复
- **参数校验增强**：文生图请求新增宽度/高度/步数范围校验；图生文缺文件参数返回 400
- **前端语法修复**：DashboardView 和 Text2ImageView Vue 组件编译错误修复
- **安全加固**：JWT 密钥/MySQL 密码支持环境变量注入；ModelController 不暴露文件绝对路径
- **推理服务稳定化**：pipeline 切换加线程锁、调度器缓存修复、种子生成优化
- **Docker 支持**：新增 `docker-compose.yml` + 三服务 Dockerfile

## 架构特点

- AI 服务采用策略模式：`Text2ImageProvider` / `Image2TextProvider` 接口
- 文生图：`LocalModelText2ImageProvider`（本地 GPU 推理）
- 图生文：`QwenImage2TextProvider`（千问 API）
- 审计日志通过 AOP 实现
- 前端路由守卫权限控制（USER / DESIGNER / ADMIN）

## 许可证

MIT
