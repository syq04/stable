# AGENTS.md — Nebula Studio

## MCP 工具使用规则
- **任何与 PowerPoint / PPT / 演示文稿相关的操作**（包括创建、编辑、排版、图表、表格、模板、主题配色、图片特效、添加动画/过渡、读取/提取文本等），一律使用 `powerpoint` 的 MCP 工具，禁止手写 python-pptx 代码
- 处理 Word 文档（.docx 创建、编辑、格式设置、批注、表格、转换 PDF）时，自动调用 `word-document-server` 的 MCP 工具
- 读取 Word 文档内容、分析文档结构时，使用 `word-document-server` 工具提取文本
- 需要浏览器操作（打开网页、截图、抓取页面内容、填写表单、自动化测试）时，自动调用 `playwright` 的 MCP 工具
- 爬取动态渲染页面、查看前端效果、调试前端页面时，优先使用 `playwright` 工具
- 搜索 GitHub 项目、查看仓库源码、查询开源项目文档、分析代码实现时，自动调用 `github` MCP 工具
- 需要了解任意 GitHub 仓库的代码结构、Issue、PR、工作流时，优先使用 `github` 工具

## 项目概览
文图互转主题设计系统。前后端分离：Vue 3 前端（端口 3000）+ Spring Boot 3 后端（端口 8080）+ Python 推理服务（端口 5000）。

## 关键目录
- `frontend/` — Vue 3 + Vite + Element Plus + Pinia
- `backend/` — Spring Boot 3.2 + Java 17 + MyBatis-Plus + Spring Security + JWT
- `inference-service/` — Python FastAPI 推理服务（diffusers 加载本地 safetensors 模型）
- `sd-models/` — **safetensors 模型文件存放目录**（7.7 GB v1-5-pruned.safetensors）
- `docs/` — 各阶段设计文档（系统思考/需求/架构/设计/原型）
- `backend/uploads/` — 图片上传存储目录
- `../test_full.py` — 全量回归测试脚本（70 项白盒+黑盒）
- `../test_report.txt` — 测试报告（自动生成）

## 开发命令
| 操作 | 命令 |
|------|------|
| 启动全部 | `start.bat`（推理服务:5000 + 后端:8080 + 前端:3000） |
| 停止全部 | `stop.bat`（停止端口 5000/8080/3000/3001 + 窗口标题匹配） |
| 构建全部 | `build.bat` (Maven clean package + npm run build) |
| 管理面板 | `dev.bat` |
| 全量回归测试 | `python F:\Stable\test_full.py` |
| 接口测试 | `test.bat` 或 `test.ps1` |
| 前端 dev | `npm run dev` (在 `frontend/` 目录) |
| 后端 mvn | `mvn spring-boot:run -Dspring-boot.run.profiles=h2` |
| 推理服务手动启动 | `python main.py` (在 `inference-service/` 目录) |
| 推理服务依赖安装 | `pip install -r requirements.txt` (在 `inference-service/` 目录) |
| 停止后端 | `Get-Process -Name java \| Stop-Process -Force` |

## 关键配置
- 默认 profile: `h2` (H2 文件数据库)，MySQL 用 `--spring.profiles.active=mysql`
- 启动后自动创建表和数据（由 `DataInitializer.java` 初始化）
- 默认管理员: `admin@nebula.com` / `admin123`
- 前端 API baseURL 硬编码在 `frontend/src/utils/request.js:6` (`http://localhost:8080/api`)
- 文件上传大小限制 50MB（单文件）/ 100MB（总请求）
- JWT 密钥在 `application.yml` jwt.secret，过期时间 24h。密钥支持环境变量注入：`${JWT_SECRET:default}`，生产环境必须设置
- MySQL 密码支持环境变量注入：`${MYSQL_USER:root}` / `${MYSQL_PASSWORD:root}`
- **2026-05-14**: `log-impl` 从 `StdOutImpl` 改为 `Slf4jImpl`，避免 SQL 日志刷屏；加了 `logging.level.com.baomidou.mybatisplus=WARN` 和 `logging.level.org.apache.ibatis=WARN`

## 文生图架构（2026-05-25 重大变更）

### 新架构：本地模型推理
```
前端 Vue 3 (:3000) → 后端 Spring Boot (:8080) → Python 推理服务 (:5000) → diffusers 加载 .safetensors 模型
```

### 推理服务 (`inference-service/main.py`)
- FastAPI 应用，端口 5000
- API 端点：
  - `GET /health` — 健康检查（返回 model_dir, models_count, device, current_model）
  - `GET /models` — 扫描 `sd-models/` 目录返回可用 .safetensors 模型列表
  - `POST /generate` — 文生图（参数：prompt, negative_prompt, width, height, steps, cfg_scale, seed, sampler_name, checkpoint_name, task_id）；支持 `callback_on_step_end` 实时记录进度
  - `GET /progress/{task_id}` — 查询生成进度（返回 step, total_steps, elapsed, its, finished）
- **启动预热**：`@app.on_event("startup")` 自动加载首个模型到 GPU，预热完成前 `/health` 不响应
- 模型加载：`StableDiffusionPipeline.from_single_file()` 从单个 safetensors 文件加载
- 设备：自动检测 CUDA（RTX 4060 Laptop 8GB），float16 推理，attention slicing 优化显存
- 模型缓存：切换 checkpoint 时重新加载，同 checkpoint 复用 pipeline
- LoRA 支持：通过 `pipe.load_lora_weights()` / `pipe.fuse_lora()` 注入
- **CUDA 兼容**：`os.environ.setdefault("CUDA_LAUNCH_BLOCKING", "1")` + `start.bat` 设 `CUDA_LAUNCH_BLOCKING=1` 解决 torch 2.5.1+cu124 与 CUDA 13.2 驱动异步死锁
- **HF 缓存**：启动时自动 `snapshot_download` 预缓存 SD 1.5 配置文件（~5MB）到本地

### Provider（Java 端）
- **仅存一个 Provider**: `LocalModelText2ImageProvider.java`（name=`local-model`）
- 通过 OkHttp 调用推理服务 `POST /generate`，接收 base64 图片
- `isAvailable()` 每 30s 缓存一次，调用 `GET /health` 检测

### 模型管理
- 模型目录：`project01/sd-models/`（通过 `ai.local-model.model-dir` 配置）
- 后端 API：`GET /api/text2image/models`（`ModelController.java` 扫描 .safetensors 文件）
- 前端：文生图页面统一使用全部可调参数（2026-05-26 删除简单模式后）

### 已删除的旧 Provider（全部 API 调用已移除）
已删除所有云端 API Provider 和本地 API Provider：ComfyUI（含 ComfyUIClient、workflow JSON）、StableDiffusion（含 SD WebUI Client）、SiliconFlow、Zhipu、HuggingFace、Pollinations、Doubao、Gemini、OpenRouter、Mock。`ComfyUI_API/` 目录已删除。

### 关键文件
- `backend/.../ai/provider/LocalModelText2ImageProvider.java` — 本地模型 Provider
- `backend/.../controller/ModelController.java` — 模型列表 API
- `backend/.../config/AiServiceConfig.java` — 推理服务配置 Bean
- `backend/.../dto/request/Text2ImageRequest.java` — 含 `checkpointName` 字段
- `backend/.../ai/provider/AiProviderManager.java` — 简化为无 fallback 逻辑
- `inference-service/main.py` — Python 推理服务（使用 `lifespan` 异步启动，含线程锁保护 pipeline 切换）
- `inference-service/requirements.txt` — Python 依赖（pinned 版本，含 `psutil`）
- `frontend/src/views/text2image/Text2ImageView.vue` — 含模型选择器

### Python 依赖版本（已验证兼容）
- torch==2.5.1+cu124（GPU，RTX 4060 Laptop 8GB，CUDA 13.2 驱动）
- torchvision==0.20.1+cu124
- diffusers==0.30.0
- transformers==4.47.0
- accelerate==0.33.0
- huggingface-hub==0.36.2
- fastapi==0.115.0
- safetensors==0.4.5
- pillow==10.4.0
- psutil==5.9.8

> **启动说明**：`start.bat` 分两步安装依赖：
> 1. `pip install torch ... --index-url https://download.pytorch.org/whl/cu124`
> 2. `pip install -r requirements.txt`（torch 不在 requirements.txt 中，避免 pip 默认拉取 CPU 版）
>
> **CUDA 异步死锁修复**：`CUDA_LAUNCH_BLOCKING=1` 必须设置在 Python 进程外部。
> `start.bat:47` 通过 `set CUDA_LAUNCH_BLOCKING=1 && python main.py` 设置，
> `main.py:10` 通过 `os.environ.setdefault` 做 fallback。

### 配置文件变更（文生图）
- `application.yml`: `ai.text2image.provider: local-model`，删除所有云端 API 配置块，新增 `ai.local-model.*` 配置
- `DataInitializer.java`: 系统配置种子数据改为 `ai.local-model.api-url` 和 `ai.local-model.model-dir`
- `AiServiceController.java`: `getProviderMessage()` 仅处理 `local-model`
- `AiServiceStatusVO.java`: 删除 `stableDiffusion` 和 `doubaoVision` 冗余字段

### 配置文件变更（图生文，2026-05-26）
- `application.yml`: `ai.image2text.provider: qwen`
- `DataInitializer.java`: 新增 `ai.qwen.api-key`（空）、`ai.qwen.api-url`（`https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`）、`ai.qwen.model`（`qwen3.5-omni-plus`）三条种子数据
- `AiServiceController.java`: `getProviderMessage()` 新增 `qwen` 分支
- `QwenImage2TextProvider.java`: 通过 `DynamicConfigService` 读取配置，不依赖 `@Value`

## 数据库表
`user`, `image_record`, `style`, `system_config`, `audit_log`

MySQL DDL 见 `backend/src/main/resources/schema.sql`，H2 见 `h2-schema.sql`

## API 约定
- 前缀: `http://localhost:8080/api/...`
- 统一响应格式: `{ code: 200, data: {...}, message: "..." }`
- 认证: `Authorization: Bearer <token>`
- 用户角色: `USER` / `DESIGNER` / `ADMIN` (RBAC 路由级控制)

## 图生文架构（2026-05-26）

### 架构
```
前端 Vue 3 (:3000) → 后端 Spring Boot (:8080) → 千问 Qwen3.5-Omni-Plus API → JSON 分析结果
```
- 图生文不再依赖 Python 推理服务（推理服务仅处理文生图 Stable Diffusion）
- 采用阿里云百炼 DashScope API，模型 `qwen3.5-omni-plus`

### 千问 Provider (`QwenImage2TextProvider.java`)
- 实现 `Image2TextProvider` 接口，name=`qwen`
- 调用 DashScope 兼容模式 API（OpenAI 兼容格式），支持图片 base64 输入
- API Key 通过「系统配置」页面管理，存入 `system_config` 表（`ai.qwen.api-key`）
- 配置项：`ai.qwen.api-key`（API Key）、`ai.qwen.api-url`（默认 `https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions`）、`ai.qwen.model`（默认 `qwen3.5-omni-plus`）
- `isAvailable()` 检查 API Key 是否已配置
- 对 API 返回的 content 做 `stripMarkdownCodeFence()` 剥离 ```json 包裹，确保纯 JSON
- 返回 JSON 格式：`{ description, tags, style, prompt }`，前端直接 `JSON.parse` 解析

### 图生文请求流程
```
POST /api/image2text/analyze
  ├── @RequestParam image (MultipartFile)
  └── @RequestParam analysisType (默认 "general", 可选)
        ↓
ImageRecordServiceImpl.image2Text(userId, image, analysisType)
        ↓
QwenImage2TextProvider.analyze(imageBytes, mimeType, analysisType)
        ↓
  ├── 图片转 base64 → data URL
  ├── 根据 analysisType 选择 system prompt（默认通用描述）
  ├── POST DashScope /compatible-mode/v1/chat/completions
  └── 剥离 markdown → 返回纯 JSON → 存入 outputContent
```

### 系统配置页面（管理员）
- 路径：`/admin/settings`（`AdminSettingsView.vue`）
- 「千问配置」区块：API Key（密码输入框，可切换显示）、API 地址、模型名称
- 配置数据通过 `DataInitializer.java` 自动初始化默认值（API Key 为空待填写）

### 前端 UI 变更（2026-05-26）
- **文生图页面**：删除「简单版」模式，只保留「专业版」全部参数；右侧面板增加实时进度条（步骤/已用/速度/剩余），通过 300ms 轮询 Python `/progress/{taskId}` 获取真实扩散步数进度（`Text2ImageView.vue`）
- **图生文页面**：顶部 `AiStatusBar` 新增 `hideT2I` prop，隐藏文生图选择器，只显示图生文选择器；删除分析类型下拉框（默认通用描述）；上传后显示模拟进度条（上传→AI分析→处理结果）（`Image2TextView.vue`）
- **AiStatusBar**: 新增 `hideT2I` prop（配对已有的 `hideI2T`），支持独立控制两组 provider 选择器的显隐
- **系统配置页**：新增「千问配置」区块替代 DeepSeek，配置 key 改为 `ai.qwen.*`（`AdminSettingsView.vue`）
- **管理员面板**：删除空壳「模型管理」页面及其路由/侧边栏菜单

### 关键文件（图生文）
- `backend/.../ai/provider/QwenImage2TextProvider.java` — 千问图生文 Provider
- `backend/.../controller/ImageController.java` — `image2Text()` 新增 `analysisType` 参数
- `backend/.../service/ImageRecordService.java` — 接口新增 `analysisType` 参数
- `backend/.../service/impl/ImageRecordServiceImpl.java` — 传递 `analysisType` 给 provider
- `frontend/src/views/image2text/Image2TextView.vue` — 图生文页面（传 `hideT2I` + `analysisType`）
- `frontend/src/views/admin/AdminSettingsView.vue` — 系统配置页（千问配置区块）
- `frontend/src/components/common/AiStatusBar.vue` — 新增 `hideT2I` prop + "qwen" 显示名

## 架构特点
- AI 服务采用策略模式: `Text2ImageProvider` / `Image2TextProvider` 接口
- 文生图实现: `LocalModelText2ImageProvider`（本地 GPU 推理）
- 图生文实现: `QwenImage2TextProvider`（千问 Qwen3.5-Omni-Plus API）
- 模型选择通过 `application.yml` 配置（`ai.text2image.provider` / `ai.image2text.provider`）
- 审计日志通过 AOP (`AuditLogAspect`) 实现
- 前端路由守卫在 `frontend/src/router/index.js` 控制权限

## 测试
- `test_full.py` — **全量回归测试**（黑盒 + 白盒），覆盖推理服务 / 后端 API / 权限 / 边界 / E2E（v1.1.0 新增）
  - 运行: `python F:\Stable\test_full.py`（依赖 `requests pillow colorama`）
  - 报告: 自动输出 `F:\Stable\test_report.txt`
  - 环境变量: `TEST_BACKEND_URL` / `TEST_INFERENCE_URL` 指定非 localhost 地址
- `test.bat` / `test.ps1` — curl/PowerShell 调用 API 进行冒烟测试
- `test_api_full.py` — Python requests 全面 API 测试
- 后端登录账户注意: `test.bat` 用 `admin@nebula.studio`，`test_api_full*.py` 用 `admin@nebula.com`

## 当前工作状态（v1.2.0 — 2026-06-10）
- **全量回归测试**：新增 `F:\Stable\test_full.py` 全量回归测试脚本，覆盖推理服务白盒/黑盒、后端 API 白盒/黑盒、权限校验、边界条件、输入校验、速率限制、端到端流程，共 70 项测试
- **后端参数校验增强**：`Text2ImageRequest.java` 新增 `@Min`/`@Max` 校验 width(64-2048) / height(64-2048) / steps(1-150)；`ImageController.java` 缺文件参数改为返回 `Result.error(400, "请上传图片文件")`；`GlobalExceptionHandler.java` 新增 `MissingServletRequestParameterException` / `MissingServletRequestPartException` 处理器
- **启动脚本修复**：`start.bat` 修复 JAR 路径 Bug（`%%~nxf` → `%%f`，缺失 `target\` 前缀导致后端闪退）；新增 H2 锁文件自动清理；`stop.bat` 新增 `ROOT` 变量并修复锁文件清理路径
- **前端语法修复**：`DashboardView.vue` `fetchStats` 函数缺少闭合 `}`；`Text2ImageView.vue` `onMounted` 箭头函数缺少闭合 `}`
- **推理服务优化**：`main.py` 加 `threading.Lock` 保护全局 pipeline 切换；修复调度器污染缓存（生成后恢复原始 scheduler）；种子生成改用 `time.time_ns()` 避免同秒重复；CORS 禁用 `allow_credentials`；`@app.on_event("startup")` 升级为 `lifespan` 异步管理器；`requirements.txt` 新增 `psutil` 且全部版本锁死
- **安全加固**：JWT 密钥和 MySQL 密码改为 `${ENV_VAR:default}` 可注入环境变量；删除 `DataInitializer` 日志中的明文密码输出；`ModelController` API 不再返回文件绝对路径
- **后端清理**：删除冗余 `AdminUsersController.java`（前端用 `/users` 非 `/admin/users`）；删除死代码 `AiServiceConfig.java`；`schema.sql` 移除重复种子数据（统一由 `DataInitializer.java` 负责）
- **前端工程化**：新增 `.env.development` / `.env.production` 环境变量文件，`vite.config.js` proxy target 支持 `VITE_PROXY_TARGET` 环境变量；安装 `eslint` + `eslint-plugin-vue`，新增 `.eslintrc.cjs` 配置；修复 `AdminDashboardView` 缺少 `qwen` provider 映射；`AppHeader` 个人信息/修改密码路由分离；10+ 处空 `catch {}` 块改为 `console.error` + 用户可见错误提示；图生文进度条标注「预估」
- **依赖升级**：Spring Boot `3.2.5` → `3.2.12`，Lombok `1.18.32` → `1.18.36`
- **Docker 支持**：新增 `docker-compose.yml` + 三服务 `Dockerfile` + frontend `nginx.conf`，支持 GPU 推理容器化一键部署
- **运行前注意**：`backend/target/`（Maven 构建产物）、`frontend/dist/` 和 `frontend/node_modules/` 不在仓库中，首次运行需 `build.bat` 构建全量产物或手动 `mvn package` / `npm install && npm run build`；Windows 环境下使用 `start.bat` / `stop.bat` 管理服务生命周期（自动清理 H2 锁文件）
- **已删除**：LoRA 训练功能（含 lora-scripts 集成、lora_model/training_task 表、前端 LoRA 页面）
- **文生图准确度评估**（2026-06-10）：新增 `POST /api/text2image/evaluate` 端点，生成图片后自动调用千问评估与原始 prompt 的匹配度，返回 0-100 准确度百分比。前端文生图页面新增「评估模式」开关，开启后按钮变为「生成并评估」，结果卡片展示准确度评分（绿>=80 / 黄>=60 / 红<60）、评分说明、图片描述、标签、风格、英文提示词。评估失败（如千问 API Key 未配置）不影响图片显示，图片仍正常展示但显示警告提示。

### 评估架构
```
前端 [评估模式] → 后端 /evaluate → [1] SD 生成图片 → [2] 千问评估 → 返回 { accuracy, accuracyDetail, analysis }
```
- `QwenImage2TextProvider.evaluateImageQuality()` — 向千问发送包含原始 prompt 的四维评分 prompt（主体40%+场景20%+风格20%+细节20%），temperature=0.3
- `Image2TextResult` 新增 `accuracy`(Double) 和 `accuracyDetail`(String) 字段
- `ImageRecordServiceImpl.evaluate()` — 生成图片→落盘→评估（容错：评估失败不影响图片返回）
- `main.py` `callback_on_step_end` 签名修复为 diffusers 0.30.0 的 4 参数格式 `(pipeline, step, timestep, callback_kwargs)`
- **评估模板格式化 Bug 修复**（2026-06-10）：`QwenImage2TextProvider.java` evaluate prompt 模板中 `占40%` 等字面 `%` 未转义为 `%%`，导致 `String.formatted()` 抛出 `UnknownFormatConversionException: Conversion = '）'`，评估始终失败。修复将所有字面 `%` 转义为 `%%`
- **评估面板 UI 优化**（2026-06-10）：移除灰色 `el-progress` 进度条，仅保留带颜色的准确度百分比文字；评估面板从图片下方移至右侧并排显示（图片 max-width:400px，评估信息 flex:1）
- **仓库清理**（2026-06-10）：删除无用文件 17 个（application-dev.yml、LoRA/admin-models 原型、旧 test/ping/PPT 脚本、运行时日志等）

## docs 结构
- `00-系统思考/system-thinking.md` — 战略思考白皮书（无需更新）
- `01-需求分析/` — 需求规格 + 用例 + 开发进度追踪表
- `02-架构设计/` — ADR + 系统架构设计
- `03-详细设计/` — 软件设计 + API 设计 + 数据库设计
- `04-页面原型/` — HTML 页面原型 + 设计规范
