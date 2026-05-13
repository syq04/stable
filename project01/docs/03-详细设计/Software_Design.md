# 文图互转主题设计系统 - 软件详细设计说明书

## 1. 分层架构设计

### 1.1 架构分层

```mermaid
graph TD
    subgraph Presentation Layer [表现层]
        Controller[REST Controllers]
        DTO[Data Transfer Objects]
    end
    
    subgraph Business Layer [业务层]
        Service[Business Services]
        ServiceImpl[Service Implementations]
        Strategy[Strategy Patterns]
    end
    
    subgraph Data Access Layer [数据访问层]
        Repository[Spring Data Repositories]
        Entity[JPA Entities]
    end
    
    subgraph Infrastructure Layer [基础设施层]
        Security[Security Config]
        Cache[Redis Cache]
        Storage[MinIO Storage]
        MQ[Kafka Messaging]
        AIModels[AI Model Clients]
    end
    
    Controller --> Service
    Service --> Repository
    Repository --> Entity
    Service --> Strategy
    Service --> Security
    Service --> Cache
    Service --> Storage
    Service --> MQ
    Service --> AIModels
    
    style Controller fill:#4ECDC4,stroke:#000
    style DTO fill:#4ECDC4,stroke:#000
    style Service fill:#45B7D1,stroke:#000
    style ServiceImpl fill:#45B7D1,stroke:#000
    style Strategy fill:#96CEB4,stroke:#000
    style Repository fill:#FFEAA7,stroke:#000
    style Entity fill:#FFEAA7,stroke:#000
    style Security fill:#DDA0DD,stroke:#000
    style Cache fill:#DDA0DD,stroke:#000
    style Storage fill:#DDA0DD,stroke:#000
    style MQ fill:#DDA0DD,stroke:#000
    style AIModels fill:#DDA0DD,stroke:#000
```

### 1.2 各层职责定义

| 层级 | 职责 | 主要组件 |
|-----|-----|---------|
| **表现层** | 处理HTTP请求/响应，参数校验，调用业务层 | Controller、DTO |
| **业务层** | 实现业务逻辑，事务管理，调用数据访问层 | Service、Strategy |
| **数据访问层** | 数据库CRUD操作，实体映射 | Repository、Entity |
| **基础设施层** | 提供基础服务（安全、缓存、存储、消息队列） | Security、Redis、MinIO、Kafka |

### 1.3 DTO定义

#### 请求DTO

| DTO名称 | 用途 | 字段 |
|--------|-----|-----|
| RegisterRequest | 用户注册 | username, email, password |
| LoginRequest | 用户登录 | email, password |
| UpdateProfileRequest | 更新个人信息 | username, email, avatarUrl |
| ChangePasswordRequest | 修改密码 | oldPassword, newPassword, confirmPassword |
| CreateUserRequest | 创建用户（管理员） | username, email, password, role |
| Text2ImageRequest | 文生图请求 | prompt, styleId, width, height, steps, cfgScale, seed |
| Image2TextRequest | 图生文请求 | image (MultipartFile) |
| CreateStyleRequest | 创建风格 | name, description, previewUrl, config |
| UpdateStyleRequest | 更新风格 | name, description, previewUrl, config |
| CreateTrainingTaskRequest | 创建训练任务 | name, params, dataPath |

#### 响应DTO

| DTO名称 | 用途 | 字段 |
|--------|-----|-----|
| UserResponse | 用户信息 | id, username, email, role, avatarUrl, createdAt |
| LoginResponse | 登录响应 | token, user |
| Text2ImageResponse | 文生图响应 | id, prompt, imageUrl, styleId, styleName, status, createdAt |
| Image2TextResponse | 图生文响应 | id, imageUrl, description, tags, status, createdAt |
| StyleResponse | 风格响应 | id, name, description, previewUrl, config, status, createdAt |
| TrainingTaskResponse | 训练任务响应 | id, name, status, params, progress, logs, createdAt, startedAt, completedAt |
| PageResponse\<T\> | 分页响应 | content, totalElements, totalPages, currentPage |

## 2. 核心类设计

### 2.1 用户管理模块类图

```mermaid
classDiagram
    class UserController {
        -UserService userService
        +register(RegisterRequest) UserResponse
        +login(LoginRequest) LoginResponse
        +logout() void
        +getProfile() UserResponse
        +updateProfile(UpdateProfileRequest) UserResponse
        +changePassword(ChangePasswordRequest) void
        +getUsers(Pageable) PageResponse~UserResponse~
        +createUser(CreateUserRequest) UserResponse
        +getUser(Long) UserResponse
        +updateUser(Long, CreateUserRequest) UserResponse
        +deleteUser(Long) void
    }
    
    class UserService {
        <<interface>>
        +register(RegisterRequest) UserResponse
        +login(LoginRequest) LoginResponse
        +logout(String) void
        +getProfile(Long) UserResponse
        +updateProfile(Long, UpdateProfileRequest) UserResponse
        +changePassword(Long, ChangePasswordRequest) void
        +getUsers(Pageable) PageResponse~UserResponse~
        +createUser(CreateUserRequest, Long) UserResponse
        +getUser(Long) UserResponse
        +updateUser(Long, CreateUserRequest, Long) UserResponse
        +deleteUser(Long) void
    }
    
    class UserServiceImpl {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        -JwtTokenProvider jwtTokenProvider
        -RedisTemplate redisTemplate
        +register(RegisterRequest) UserResponse
        +login(LoginRequest) LoginResponse
        +logout(String) void
        +getProfile(Long) UserResponse
        +updateProfile(Long, UpdateProfileRequest) UserResponse
        +changePassword(Long, ChangePasswordRequest) void
        +getUsers(Pageable) PageResponse~UserResponse~
        +createUser(CreateUserRequest, Long) UserResponse
        +getUser(Long) UserResponse
        +updateUser(Long, CreateUserRequest, Long) UserResponse
        +deleteUser(Long) void
    }
    
    class UserRepository {
        <<interface>>
        +findByUsername(String) Optional~User~
        +findByEmail(String) Optional~User~
        +existsByUsername(String) boolean
        +existsByEmail(String) boolean
    }
    
    class JwtTokenProvider {
        -String secret
        -long expireHours
        +generateToken(User) String
        +validateToken(String) boolean
        +getUserIdFromToken(String) Long
    }
    
    UserController --> UserService
    UserService <|.. UserServiceImpl
    UserServiceImpl --> UserRepository
    UserServiceImpl --> PasswordEncoder
    UserServiceImpl --> JwtTokenProvider
    UserServiceImpl --> RedisTemplate
```

### 2.2 文生图模块类图

```mermaid
classDiagram
    class Text2ImageController {
        -Text2ImageService text2ImageService
        +generate(Text2ImageRequest) Text2ImageResponse
        +getHistory(Pageable) PageResponse~Text2ImageResponse~
        +getRecord(Long) Text2ImageResponse
        +deleteRecord(Long) void
    }
    
    class Text2ImageService {
        <<interface>>
        +generate(Text2ImageRequest, Long) Text2ImageResponse
        +getHistory(Long, Pageable) PageResponse~Text2ImageResponse~
        +getRecord(Long, Long) Text2ImageResponse
        +deleteRecord(Long, Long) void
    }
    
    class Text2ImageServiceImpl {
        -ImageRecordRepository imageRecordRepository
        -StyleRepository styleRepository
        -AiProviderManager aiProviderManager
        -LocalFileService localFileService
        +generate(Text2ImageRequest, Long) Text2ImageResponse
        +getHistory(Long, Pageable) PageResponse~Text2ImageResponse~
        +getRecord(Long, Long) Text2ImageResponse
        +deleteRecord(Long, Long) void
    }
    
    class Text2ImageProvider {
        <<interface>>
        +String getName()
        +boolean isAvailable()
        +GenerateResult generate(Text2ImageRequest, Style)
    }
    
    class ComfyUIText2ImageProvider {
        -ComfyUIClient comfyUIClient
        +generate(Text2ImageRequest, Style) GenerateResult
    }
    
    class SiliconFlowText2ImageProvider {
        +generate(Text2ImageRequest, Style) GenerateResult
    }
    
    class ZhipuText2ImageProvider {
        +generate(Text2ImageRequest, Style) GenerateResult
    }
    
    class PollinationsText2ImageProvider {
        +generate(Text2ImageRequest, Style) GenerateResult
    }
    
    class MockText2ImageProvider {
        +generate(Text2ImageRequest, Style) GenerateResult
    }
    
    class AiProviderManager {
        -List~Text2ImageProvider~ text2imageProviders
        -List~Image2TextProvider~ image2textProviders
        +getActiveText2ImageProvider() Text2ImageProvider
        +switchText2ImageProvider(String) void
        +getText2ImageProviderStatus() List~ProviderStatus~
    }
    
    class ComfyUIClient {
        -String apiUrl
        +isAvailable() boolean
        +generate(Text2ImageRequest) byte[]
        +loadWorkflow() JsonNode
        +injectParams(JsonNode, Text2ImageRequest) JsonNode
        +waitForCompletion(String) JsonNode
        +downloadFirstImage(JsonNode, String) byte[]
    }
    
    Text2ImageController --> Text2ImageService
    Text2ImageService <|.. Text2ImageServiceImpl
    Text2ImageServiceImpl --> ImageRecordRepository
    Text2ImageServiceImpl --> AiProviderManager
    Text2ImageServiceImpl --> LocalFileService
    AiProviderManager --> Text2ImageProvider
    Text2ImageProvider <|.. ComfyUIText2ImageProvider
    Text2ImageProvider <|.. SiliconFlowText2ImageProvider
    Text2ImageProvider <|.. ZhipuText2ImageProvider
    Text2ImageProvider <|.. PollinationsText2ImageProvider
    Text2ImageProvider <|.. MockText2ImageProvider
    ComfyUIText2ImageProvider --> ComfyUIClient
```

### 2.3 LoRA训练模块类图

```mermaid
classDiagram
    class LoRAController {
        -LoRAService loraService
        +createTask(CreateTrainingTaskRequest) TrainingTaskResponse
        +getTasks(Pageable) PageResponse~TrainingTaskResponse~
        +getTask(Long) TrainingTaskResponse
        +startTask(Long) TrainingTaskResponse
        +getLogs(Long) String
        +downloadModel(Long) byte[]
        +deleteTask(Long) void
    }
    
    class LoRAService {
        <<interface>>
        +createTask(CreateTrainingTaskRequest, Long) TrainingTaskResponse
        +getTasks(Long, Pageable) PageResponse~TrainingTaskResponse~
        +getTask(Long, Long) TrainingTaskResponse
        +startTask(Long, Long) TrainingTaskResponse
        +getLogs(Long) String
        +downloadModel(Long) byte[]
        +deleteTask(Long, Long) void
    }
    
    class LoRAServiceImpl {
        -TrainingTaskRepository trainingTaskRepository
        -LoraModelRepository loraModelRepository
        -KafkaTemplate kafkaTemplate
        -MinioService minioService
        +createTask(CreateTrainingTaskRequest, Long) TrainingTaskResponse
        +getTasks(Long, Pageable) PageResponse~TrainingTaskResponse~
        +getTask(Long, Long) TrainingTaskResponse
        +startTask(Long, Long) TrainingTaskResponse
        +getLogs(Long) String
        +downloadModel(Long) byte[]
        +deleteTask(Long, Long) void
    }
    
    class TrainingTaskConsumer {
        -TrainingTaskRepository trainingTaskRepository
        -LoraModelRepository loraModelRepository
        -LoRATrainer loraTrainer
        +consume(TrainingTaskEvent) void
    }
    
    class LoRATrainer {
        +train(String, Map~String, Object~) TrainingResult
    }
    
    LoRAController --> LoRAService
    LoRAService <|.. LoRAServiceImpl
    LoRAServiceImpl --> TrainingTaskRepository
    LoRAServiceImpl --> LoraModelRepository
    LoRAServiceImpl --> KafkaTemplate
    LoRAServiceImpl --> MinioService
    TrainingTaskConsumer --> TrainingTaskRepository
    TrainingTaskConsumer --> LoraModelRepository
    TrainingTaskConsumer --> LoRATrainer
```

## 3. 设计模式应用

### 3.1 策略模式 - AI模型调用

**应用场景**: 支持多种AI模型（ComfyUI、SiliconFlow、智谱AI、HuggingFace、Pollinations等）进行文生图和图生文操作

```mermaid
graph TD
    A[Text2ImageServiceImpl] --> B{AiProviderManager}
    B -->|getActiveText2ImageProvider()| C[当前活跃 Provider]
    C -->|ComfyUI| D[ComfyUIText2ImageProvider]
    C -->|SiliconFlow| E[SiliconFlowText2ImageProvider]
    C -->|智谱AI| F[ZhipuText2ImageProvider]
    C -->|Pollinations| G[PollinationsText2ImageProvider]
    C -->|Mock| H[MockText2ImageProvider]
    
    D --> I[ComfyUIClient.generate]
    I --> I1[loadWorkflow 加载JSON模板]
    I1 --> I2[injectParams 注入参数到节点]
    I2 --> I3[POST /prompt 提交工作流]
    I3 --> I4[轮询 /history 等待完成]
    I4 --> I5[GET /view 下载图片]
    I5 --> J[返回图片数据]
    
    E --> J
    F --> J
    G --> J
    H --> J
    
    J --> K[保存到本地 uploads 目录]
```

**设计理由**:
- `Text2ImageProvider` 接口定义 `generate()` / `isAvailable()` / `getName()`
- 各 Provider 通过 `@Component` 自动注入到 `AiProviderManager`
- `AiProviderManager` 管理当前活跃 Provider，支持运行时切换
- 前端 `AiStatusBar` 下拉框切换 → `POST /api/ai/text2image/switch` → 即时生效
- 符合开闭原则（对扩展开放，对修改关闭）

### 3.2 ComfyUI 客户端设计

**应用场景**: 通过 HTTP API 调用本地的 ComfyUI 推理引擎，基于 workflow 工作流模板生成图片

```java
// 核心调用流程
public class ComfyUIClient {
    // 1. 加载 workflow JSON 模板
    JsonNode workflow = loadWorkflow("comfyui_default.json");
    
    // 2. 注入参数到工作流节点
    //    节点 26: 正向提示词 (CLIPTextEncode)
    //    节点 22: 负向提示词 (CLIPTextEncode)
    //    节点 24: 采样参数 (KSampler) — steps, cfgScale, seed, samplerName
    //    节点 25: 图片尺寸 (EmptyLatentImage) — width, height
    JsonNode prompt = injectParams(workflow, request);
    
    // 3. 提交工作流，获取 prompt_id
    String promptId = submitPrompt(prompt);
    
    // 4. 轮询等待执行完成
    JsonNode history = waitForCompletion(promptId);
    
    // 5. 从 outputs 解析图片并下载
    byte[] image = downloadFirstImage(history, apiUrl);
}
```

**关键设计点**:
- Workflow JSON 模板位于 `resources/workflow/comfyui_default.json`
- API 地址通过 `DynamicConfigService` 运行时读取（DB → yml 兜底），支持管理后台修改即时生效
- 超时控制：默认 120s（`ai.comfyui.timeout`），轮询间隔 500ms
- 文件存储：图片下载后保存到 `backend/uploads/text2image/{date}/` 目录

### 3.3 模板方法模式 - 统一处理流程

**应用场景**: 文生图和图生文的处理流程有相似的步骤（参数校验、调用AI、保存结果）

```mermaid
flowchart TD
    A[开始] --> B{任务类型}
    B -->|文生图| C[Text2ImageService.generate]
    B -->|图生文| D[Image2TextService.analyze]
    
    C --> E[参数校验]
    D --> E
    
    E --> F{校验通过?}
    F -->|否| G[抛出参数异常]
    F -->|是| H[获取风格配置]
    
    H --> I[调用AI模型]
    I --> J{调用成功?}
    J -->|否| K[更新状态为FAILED]
    J -->|是| L[保存结果到MinIO]
    
    L --> M[保存记录到数据库]
    K --> M
    
    M --> N[返回响应]
    N --> O[结束]
```

## 4. 交互流程时序图

### 4.1 用户登录流程

```mermaid
sequenceDiagram
    participant Client as 前端
    participant Controller as AuthController
    participant Service as UserServiceImpl
    participant Repo as UserRepository
    participant Encoder as PasswordEncoder
    participant JWT as JwtTokenProvider
    participant Cache as Redis
    
    Client->>Controller: POST /api/auth/login
    Note over Client,Controller: {"email": "user@example.com", "password": "xxx"}
    
    Controller->>Service: login(request)
    Service->>Repo: findByEmail(email)
    
    alt 用户存在
        Repo-->>Service: Optional~User~
        Service->>Encoder: matches(rawPassword, encodedPassword)
        
        alt 密码匹配
            Encoder-->>Service: true
            Service->>JWT: generateToken(user)
            JWT-->>Service: token
            
            Service->>Cache: set("token:" + userId, token, expireTime)
            Cache-->>Service: OK
            
            Service-->>Controller: LoginResponse
            Controller-->>Client: 200 OK {"code": 200, "data": {...}}
        else 密码不匹配
            Encoder-->>Service: false
            Service-->>Controller: 抛出AuthenticationException
            Controller-->>Client: 401 Unauthorized {"code": 1002, "message": "密码错误"}
        end
    else 用户不存在
        Repo-->>Service: Optional.empty()
        Service-->>Controller: 抛出UserNotFoundException
        Controller-->>Client: 404 Not Found {"code": 1001, "message": "用户不存在"}
    end
```

### 4.2 文生图流程

```mermaid
sequenceDiagram
    participant Client as 前端
    participant Controller as Text2ImageController
    participant Service as Text2ImageServiceImpl
    participant Manager as AiProviderManager
    participant Provider as Text2ImageProvider
    participant FileService as LocalFileService
    participant Repo as ImageRecordRepository
    participant StyleRepo as StyleRepository
    
    Client->>Controller: POST /api/text2image/generate
    Note over Client,Controller: {"prompt": "一只猫", "styleId": 1, ...}
    
    Controller->>Service: generate(request, userId)
    
    Service->>StyleRepo: findById(styleId)
    StyleRepo-->>Service: Style (可选)
    
    Service->>Manager: getActiveText2ImageProvider()
    Manager-->>Service: Text2ImageProvider (当前活跃)
    
    Service->>Provider: generate(request, style)
    
    alt Provider is ComfyUI
        Note over Provider: ComfyUIText2ImageProvider
        Provider->>Provider: loadWorkflow + injectParams
        Provider->>Provider: POST /prompt (ComfyUI)
        Provider->>Provider: 轮询等待 /history
        Provider->>Provider: GET /view 下载图片
        Provider-->>Service: byte[] imageData
    else Provider is SiliconFlow
        Provider->>Provider: 调用SiliconFlow API
        Provider-->>Service: byte[] imageData
    else Provider is Mock
        Provider->>Provider: 生成占位图
        Provider-->>Service: byte[] imageData
    end
    
    Service->>FileService: saveImage(imageData, filename)
    FileService-->>Service: imageUrl (相对路径)
    
    Service->>Repo: save(ImageRecord)
    Repo-->>Service: ImageRecord
    
    Service-->>Controller: Text2ImageResponse
    Controller-->>Client: 200 OK {"code": 200, "data": {...}}
```

### 4.3 LoRA训练流程

```mermaid
sequenceDiagram
    participant Client as 前端
    participant Controller as LoRAController
    participant Service as LoRAServiceImpl
    participant TaskRepo as TrainingTaskRepository
    participant Kafka as KafkaTemplate
    participant Consumer as TrainingTaskConsumer
    participant Trainer as LoRATrainer
    participant Minio as MinioService
    participant ModelRepo as LoraModelRepository
    
    Client->>Controller: POST /api/lora/tasks
    Controller->>Service: createTask(request, userId)
    Service->>TaskRepo: save(TrainingTask)
    TaskRepo-->>Service: TrainingTask (status=PENDING)
    Service-->>Controller: TrainingTaskResponse
    Controller-->>Client: 200 OK {"code": 200, "data": {...}}
    
    Client->>Controller: PUT /api/lora/tasks/{id}/start
    Controller->>Service: startTask(taskId, userId)
    Service->>TaskRepo: findById(taskId)
    TaskRepo-->>Service: TrainingTask
    Service->>TaskRepo: 更新状态为RUNNING
    Service->>Kafka: send("lora-training", TrainingTaskEvent)
    Kafka-->>Service: OK
    Service-->>Controller: TrainingTaskResponse
    Controller-->>Client: 200 OK {"code": 200, "message": "训练已启动"}
    
    Kafka->>Consumer: consume(TrainingTaskEvent)
    Consumer->>TaskRepo: 更新状态为RUNNING
    Consumer->>Trainer: train(dataPath, params)
    
    loop 训练中
        Trainer-->>Consumer: progress(0-100)
        Consumer->>TaskRepo: 更新进度
    end
    
    Trainer-->>Consumer: TrainingResult
    Consumer->>Minio: upload(modelFile, path)
    Minio-->>Consumer: modelPath
    Consumer->>ModelRepo: save(LoraModel)
    Consumer->>TaskRepo: 更新状态为COMPLETED
```

## 5. 异常处理设计

### 5.1 异常类体系

```mermaid
classDiagram
    class RuntimeException {
        <<Java>>
    }
    
    class BusinessException {
        -Integer code
        -String message
        +BusinessException(Integer, String)
        +getCode() Integer
    }
    
    class UserException {
        <<interface>>
    }
    
    class AuthException {
        <<interface>>
    }
    
    class ResourceException {
        <<interface>>
    }
    
    class UserNotFoundException {
        +UserNotFoundException(String)
    }
    
    class UserAlreadyExistsException {
        +UserAlreadyExistsException(String)
    }
    
    class AuthenticationException {
        +AuthenticationException(String)
    }
    
    class AuthorizationException {
        +AuthorizationException(String)
    }
    
    class ResourceNotFoundException {
        +ResourceNotFoundException(String)
    }
    
    class ResourceAlreadyExistsException {
        +ResourceAlreadyExistsException(String)
    }
    
    class AICallException {
        +AICallException(String)
    }
    
    class ValidationException {
        +ValidationException(String)
    }
    
    RuntimeException <|-- BusinessException
    BusinessException <|.. UserException
    BusinessException <|.. AuthException
    BusinessException <|.. ResourceException
    UserException <|-- UserNotFoundException
    UserException <|-- UserAlreadyExistsException
    AuthException <|-- AuthenticationException
    AuthException <|-- AuthorizationException
    ResourceException <|-- ResourceNotFoundException
    ResourceException <|-- ResourceAlreadyExistsException
    BusinessException <|-- AICallException
    BusinessException <|-- ValidationException
```

### 5.2 全局异常处理器

```java
// 伪代码示例
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity.badRequest()
            .body(Result.error(e.getCode(), e.getMessage()));
    }
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Result<Void>> handleResourceNotFoundException(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Result.error(404, e.getMessage()));
    }
    
    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<Result<Void>> handleAuthorizationException(AuthorizationException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(Result.error(403, e.getMessage()));
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
            .body(Result.error(400, message));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
        // 记录日志
        log.error("Unexpected error", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.error(500, "服务器内部错误"));
    }
}
```

### 5.3 错误码规范

| 错误码范围 | 类别 | 说明 |
|----------|-----|-----|
| 1001-1999 | 用户相关 | 用户不存在、用户已存在、密码错误等 |
| 2001-2999 | 资源相关 | 资源不存在、资源已存在等 |
| 3001-3999 | 业务逻辑 | 训练任务不存在、任务已在运行等 |
| 4001-4999 | 文件操作 | 文件上传失败、文件不存在等 |
| 5001-5999 | 外部服务 | AI模型调用失败、存储服务异常等 |
| 6001-6999 | 系统错误 | 数据库异常、缓存异常等 |

## 6. 安全设计

### 6.1 认证流程

```mermaid
flowchart TD
    A[用户请求] --> B[JwtAuthenticationFilter]
    B --> C{请求头包含Token?}
    
    C -->|否| D[放行到登录接口]
    D --> E[AuthController]
    
    C -->|是| F[验证Token签名]
    F --> G{签名有效?}
    
    G -->|否| H[返回401 Unauthorized]
    
    G -->|是| I[解析用户信息]
    I --> J[检查Token是否在黑名单]
    J --> K{在黑名单?}
    
    K -->|是| H
    
    K -->|否| L[设置SecurityContext]
    L --> M[继续处理请求]
```

### 6.2 授权流程

```mermaid
flowchart TD
    A[用户请求] --> B[MethodSecurityInterceptor]
    B --> C{检查@PreAuthorize注解}
    
    C -->|无注解| D[允许访问]
    
    C -->|有注解| E[解析权限表达式]
    E --> F[获取当前用户角色]
    F --> G{角色匹配?}
    
    G -->|是| D
    
    G -->|否| H[返回403 Forbidden]
```

### 6.3 Spring Security配置结构

| 配置项 | 说明 |
|-------|-----|
| JwtAuthenticationFilter | JWT Token过滤器，验证Token有效性 |
| JwtAuthorizationFilter | 授权过滤器，检查用户权限 |
| UserDetailsServiceImpl | 用户详情服务，从数据库加载用户 |
| PasswordEncoder | BCrypt密码编码器 |
| SecurityFilterChain | 安全过滤链配置 |

---

**文档版本**: v1.0  
**创建日期**: 2026-05-10  
**适用框架**: Spring Boot 3.2.x
