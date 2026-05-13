# AGENTS.md — Nebula Studio

## 项目概览
文图互转主题设计系统。前后端分离：Vue 3 前端（端口 3000）+ Spring Boot 3 后端（端口 8080）。

## 关键目录
- `frontend/` — Vue 3 + Vite + Element Plus + Pinia
- `backend/` — Spring Boot 3.2 + Java 17 + MyBatis-Plus + Spring Security + JWT
- `docs/` — 各阶段设计文档（系统思考/需求/架构/设计/原型）
- `backend/uploads/` — 图片上传存储目录

## 开发命令
| 操作 | 命令 |
|------|------|
| 构建全部 | `build.bat` (Maven clean package + npm run build) |
| 启动服务 | `start.bat` 或 `start.ps1` |
| 停止服务 | `stop.bat` 或 `stop.ps1` |
| 管理面板 | `dev.bat` |
| 接口测试 | `test.bat` 或 `test.ps1` |
| 前端 dev | `npm run dev` (在 `frontend/` 目录) |
| 后端 mvn | `mvn spring-boot:run -Dspring-boot.run.profiles=h2` |

## 关键配置
- 默认 profile: `h2` (H2 文件数据库)，MySQL 用 `--spring.profiles.active=mysql`
- 启动后自动创建表和数据（由 `DataInitializer.java` 初始化）
- 默认管理员: `admin@nebula.com` / `admin123`
- 前端 API baseURL 硬编码在 `frontend/src/utils/request.js:6` (`http://localhost:8080/api`)
- 文件上传大小限制 50MB（单文件）/ 100MB（总请求）
- JWT 密钥在 `application.yml` jwt.secret，过期时间 24h

## 数据库表
`user`, `image_record`, `style`, `training_task`, `lora_model`, `system_config`, `audit_log`

MySQL DDL 见 `backend/src/main/resources/schema.sql`，H2 见 `h2-schema.sql`

## API 约定
- 前缀: `http://localhost:8080/api/...`
- 统一响应格式: `{ code: 200, data: {...}, message: "..." }`
- 认证: `Authorization: Bearer <token>`
- 用户角色: `USER` / `DESIGNER` / `ADMIN` (RBAC 路由级控制)

## 架构特点
- AI 服务采用策略模式: `Text2ImageProvider` / `Image2TextProvider` 接口
- 各实现类在 `backend/src/.../ai/provider/` 目录
- 模型选择通过 `application.yml` 配置（`ai.text2image.provider` / `ai.image2text.provider`）
- 审计日志通过 AOP (`AuditLogAspect`) 实现
- 前端路由守卫在 `frontend/src/router/index.js` 控制权限

## 测试
- `test.bat` / `test.ps1` — curl/PowerShell 调用 API 进行冒烟测试
- `test_api_full.py` — Python requests 全面 API 测试
- `backend/test.ps1` — 后端 AI 状态测试
- 后端登录账户注意: `test.bat` 用 `admin@nebula.studio`，`test_api_full*.py` 用 `admin@nebula.com`
- 单元测试: `ComfyUIClientTest.java` (13个), `ComfyUIText2ImageProviderTest.java` (7个) — 均在 `backend/src/test/`

## ComfyUI 集成

### Provider 配置
- provider name: `comfyui`（可在前端 AiStatusBar 切换）
- 默认 API 地址: `http://127.0.0.1:8188`（通过 `ai.comfyui.api-url` 在 yml 或 system_config 表配置）
- 默认 enabled（`ai.comfyui.enabled: true`），ComfyUI 服务需在 `http://127.0.0.1:8188` 运行

### 实现文件
- `ComfyUIClient.java` — 封装 ComfyUI HTTP 协议（`POST /prompt` → 轮询 `GET /history/{id}` → `GET /view`）
- `ComfyUIText2ImageProvider.java` — 实现 `Text2ImageProvider` 接口

### 动态配置
- API 地址通过 `DynamicConfigService` 读取，优先 DB（system_config 表 `ai.comfyui.api-url`），兜底 application.yml
- 前端管理后台「系统配置 → ComfyUI API 地址」可运行时修改，即时生效

### 工作流
- 默认 workflow 文件: `backend/src/main/resources/workflow/comfyui_default.json`
- 节点映射: 26=正向提示词, 22=负向提示词, 24=采样参数, 25=图片尺寸

## docs 结构
- `00-系统思考/system-thinking.md` — 战略思考白皮书（无需更新）
- `01-需求分析/` — 需求规格 + 用例 + 开发进度追踪表（含 F07 ComfyUI 集成，已更新）
- `02-架构设计/` — ADR（含 ADR-009 ComfyUI）+ 系统架构设计（含 ComfyUI 工作流机制）
- `03-详细设计/` — 软件设计（含实际 Text2ImageProvider 类图 + ComfyUIClient 设计）+ API 设计（含 AI 状态接口）+ 数据库设计（含 ComfyUI 种子数据）
- `04-页面原型/` — HTML 页面原型 + 设计规范
