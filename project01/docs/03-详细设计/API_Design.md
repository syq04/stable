# 文图互转主题设计系统 - API接口设计说明书

## 1. 接口设计规范

### 1.1 统一响应结构

所有API接口返回统一的JSON格式：

```json
{
    "code": 200,
    "message": "success",
    "data": {},
    "timestamp": 1715318400000
}
```

| 字段 | 类型 | 说明 |
|-----|-----|-----|
| code | Integer | 业务状态码，200表示成功，其他为错误码 |
| message | String | 响应消息 |
| data | Object | 响应数据，成功时返回具体数据，失败时可能为null |
| timestamp | Long | 响应时间戳（毫秒） |

### 1.2 状态码规范

| HTTP状态码 | 业务状态码 | 说明 |
|-----------|-----------|-----|
| 200 | 200 | 请求成功 |
| 400 | 400 | 请求参数错误 |
| 401 | 401 | 未授权，需要登录 |
| 403 | 403 | 权限不足 |
| 404 | 404 | 资源不存在 |
| 500 | 500 | 服务器内部错误 |

### 1.3 错误码定义

| 错误码 | 说明 |
|-----|-----|
| 1001 | 用户不存在 |
| 1002 | 密码错误 |
| 1003 | 用户已存在 |
| 1004 | 邮箱已被注册 |
| 2001 | 风格不存在 |
| 2002 | 风格名称已存在 |
| 3001 | 训练任务不存在 |
| 4001 | 文件上传失败 |
| 5001 | AI模型调用失败 |
| 5002 | ComfyUI 连接失败 |
| 5003 | ComfyUI 未返回图片数据 |

### 1.4 认证方式

所有需要登录的接口通过 `Authorization` 请求头传递 JWT Token：

```
Authorization: Bearer eyJhbGciOiJIUzUxMiJ9...
```

---

## 2. AI 服务状态 API

### 2.1 接口列表

| HTTP方法 | 路径 | 描述 | 权限 |
|---------|-----|-----|-----|
| GET | /api/ai/providers | 获取所有 Provider 状态 | 已登录 |
| GET | /api/ai/status | 获取当前 AI 服务状态 | 已登录 |
| PUT | /api/ai/text2image/switch | 切换文生图 Provider | 已登录 |
| PUT | /api/ai/image2text/switch | 切换图生文 Provider | 已登录 |

### 2.2 接口详情

#### GET /api/ai/providers - 获取所有 Provider 状态

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "activeText2Image": "comfyui",
        "text2image": [
            {"name": "comfyui", "available": true, "active": true},
            {"name": "siliconflow", "available": false, "active": false},
            {"name": "zhipu", "available": false, "active": false},
            {"name": "pollinations", "available": true, "active": false},
            {"name": "mock", "available": true, "active": false}
        ],
        "activeImage2Text": "mock",
        "image2text": [
            {"name": "mock", "available": true, "active": true}
        ]
    },
    "timestamp": 1715318400000
}
```

| 字段 | 类型 | 说明 |
|-----|-----|------|
| activeText2Image | String | 当前活跃的文生图 Provider 名称 |
| text2image | Array | 所有文生图 Provider 的可用性和活跃状态 |
| activeImage2Text | String | 当前活跃的图生文 Provider 名称 |
| image2text | Array | 所有图生文 Provider 的可用性和活跃状态 |

#### GET /api/ai/status - 获取 AI 服务状态

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "text2image": {
            "activeProvider": "comfyui",
            "status": "AVAILABLE",
            "providers": [
                {"name": "comfyui", "status": "AVAILABLE"},
                {"name": "siliconflow", "status": "UNAVAILABLE"},
                {"name": "pollinations", "status": "AVAILABLE"},
                {"name": "mock", "status": "AVAILABLE"}
            ]
        },
        "image2text": {
            "activeProvider": "mock",
            "status": "AVAILABLE",
            "providers": [
                {"name": "mock", "status": "AVAILABLE"}
            ]
        }
    },
    "timestamp": 1715318400000
}
```

#### PUT /api/ai/text2image/switch - 切换文生图 Provider

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
    "provider": "comfyui"
}
```

**成功响应**:
```json
{
    "code": 200,
    "message": "文生图模型已切换为: ComfyUI",
    "data": {
        "previousProvider": "pollinations",
        "currentProvider": "comfyui"
    },
    "timestamp": 1715318400000
}
```

#### PUT /api/ai/image2text/switch - 切换图生文 Provider

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
    "provider": "mock"
}
```

**成功响应**:
```json
{
    "code": 200,
    "message": "图生文模型已切换为: 模拟模式",
    "data": {
        "previousProvider": "mock",
        "currentProvider": "mock"
    },
    "timestamp": 1715318400000
}
```

---

## 3. 用户管理模块 API

### 2.1 接口列表

| HTTP方法 | 路径 | 描述 | 权限 |
|---------|-----|-----|-----|
| POST | /api/auth/register | 用户注册 | 匿名 |
| POST | /api/auth/login | 用户登录 | 匿名 |
| POST | /api/auth/logout | 用户退出 | 已登录 |
| GET | /api/user/profile | 获取个人信息 | 已登录 |
| PUT | /api/user/profile | 更新个人信息 | 已登录 |
| PUT | /api/user/password | 修改密码 | 已登录 |
| GET | /api/users | 获取用户列表 | 管理员 |
| POST | /api/users | 创建用户 | 管理员 |
| GET | /api/users/{id} | 获取用户详情 | 管理员 |
| PUT | /api/users/{id} | 更新用户 | 管理员 |
| DELETE | /api/users/{id} | 删除用户 | 管理员 |

### 2.2 接口详情

#### POST /api/auth/register - 用户注册

**请求体**:
```json
{
    "username": "string (必填，用户名，3-64字符)",
    "email": "string (必填，邮箱格式)",
    "password": "string (必填，密码，6-128字符)"
}
```

**成功响应**:
```json
{
    "code": 200,
    "message": "注册成功",
    "data": {
        "id": 1,
        "username": "testuser",
        "email": "test@example.com",
        "role": "USER",
        "createdAt": "2026-05-10T10:00:00"
    },
    "timestamp": 1715318400000
}
```

#### POST /api/auth/login - 用户登录

**请求体**:
```json
{
    "email": "string (必填，邮箱)",
    "password": "string (必填，密码)"
}
```

**成功响应**:
```json
{
    "code": 200,
    "message": "登录成功",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "user": {
            "id": 1,
            "username": "testuser",
            "email": "test@example.com",
            "role": "USER"
        }
    },
    "timestamp": 1715318400000
}
```

#### GET /api/user/profile - 获取个人信息

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "id": 1,
        "username": "testuser",
        "email": "test@example.com",
        "role": "USER",
        "avatarUrl": "https://example.com/avatar.jpg",
        "createdAt": "2026-05-10T10:00:00"
    },
    "timestamp": 1715318400000
}
```

#### PUT /api/user/profile - 更新个人信息

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
    "username": "string (选填，新用户名)",
    "email": "string (选填，新邮箱)",
    "avatarUrl": "string (选填，头像URL)"
}
```

**成功响应**:
```json
{
    "code": 200,
    "message": "更新成功",
    "data": {
        "id": 1,
        "username": "newname",
        "email": "new@example.com",
        "role": "USER",
        "avatarUrl": "https://example.com/new-avatar.jpg"
    },
    "timestamp": 1715318400000
}
```

#### PUT /api/user/password - 修改密码

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
    "oldPassword": "string (必填，原密码)",
    "newPassword": "string (必填，新密码，6-128字符)",
    "confirmPassword": "string (必填，确认新密码)"
}
```

**成功响应**:
```json
{
    "code": 200,
    "message": "密码修改成功",
    "data": null,
    "timestamp": 1715318400000
}
```

#### GET /api/users - 获取用户列表（管理员）

**请求头**: `Authorization: Bearer {token}`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|-----|-----|-----|-----|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认20 |
| role | String | 否 | 角色筛选 |
| keyword | String | 否 | 关键词搜索（用户名/邮箱） |

**成功响应**:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "content": [
            {
                "id": 1,
                "username": "admin",
                "email": "admin@example.com",
                "role": "ADMIN",
                "createdAt": "2026-05-10T10:00:00"
            }
        ],
        "totalElements": 100,
        "totalPages": 5,
        "currentPage": 1
    },
    "timestamp": 1715318400000
}
```

## 4. 文生图模块 API

### 3.1 接口列表

| HTTP方法 | 路径 | 描述 | 权限 |
|---------|-----|-----|-----|
| POST | /api/text2image/generate | 生成图像 | 已登录 |
| GET | /api/text2image/history | 获取生成历史 | 已登录 |
| GET | /api/text2image/{id} | 获取生成记录详情 | 已登录 |
| DELETE | /api/text2image/{id} | 删除生成记录 | 已登录 |

### 3.2 接口详情

#### POST /api/text2image/generate - 生成图像

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
    "prompt": "string (必填，文本描述，10-500字符)",
    "styleId": "Long (选填，风格ID)",
    "width": "Integer (选填，图像宽度，默认512)",
    "height": "Integer (选填，图像高度，默认512)",
    "steps": "Integer (选填，生成步数，默认20)",
    "cfgScale": "Float (选填，CFG比例，默认7.5)",
    "seed": "Long (选填，随机种子，默认-1自动生成)"
}
```

**成功响应**:
```json
{
    "code": 200,
    "message": "生成成功",
    "data": {
        "id": 1,
        "prompt": "一只可爱的猫咪在花园里",
        "imageUrl": "https://example.com/generated-image.png",
        "styleId": 1,
        "styleName": "写实风格",
        "status": "SUCCESS",
        "createdAt": "2026-05-10T10:00:00"
    },
    "timestamp": 1715318400000
}
```

#### GET /api/text2image/history - 获取生成历史

**请求头**: `Authorization: Bearer {token}`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|-----|-----|-----|-----|
| page | Integer | 否 | 页码，默认1 |
| size | Integer | 否 | 每页数量，默认20 |

**成功响应**:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "content": [
            {
                "id": 1,
                "prompt": "一只可爱的猫咪",
                "imageUrl": "https://example.com/image.png",
                "styleName": "写实风格",
                "status": "SUCCESS",
                "createdAt": "2026-05-10T10:00:00"
            }
        ],
        "totalElements": 50,
        "totalPages": 3,
        "currentPage": 1
    },
    "timestamp": 1715318400000
}
```

## 5. 图生文模块 API

### 4.1 接口列表

| HTTP方法 | 路径 | 描述 | 权限 |
|---------|-----|-----|-----|
| POST | /api/image2text/analyze | 分析图像 | 已登录 |
| GET | /api/image2text/history | 获取分析历史 | 已登录 |
| GET | /api/image2text/{id} | 获取分析记录详情 | 已登录 |
| PUT | /api/image2text/{id} | 更新分析结果 | 已登录 |
| DELETE | /api/image2text/{id} | 删除分析记录 | 已登录 |

### 4.2 接口详情

#### POST /api/image2text/analyze - 分析图像

**请求头**: `Authorization: Bearer {token}`

**Content-Type**: `multipart/form-data`

| 参数 | 类型 | 必填 | 说明 |
|-----|-----|-----|-----|
| image | File | 是 | 图像文件（JPG/PNG/WebP，<10MB） |

**成功响应**:
```json
{
    "code": 200,
    "message": "分析成功",
    "data": {
        "id": 1,
        "imageUrl": "https://example.com/uploaded-image.png",
        "description": "这是一张展示城市夜景的照片，画面中有高楼大厦和车流灯光...",
        "tags": ["城市", "夜景", "建筑", "灯光"],
        "status": "SUCCESS",
        "createdAt": "2026-05-10T10:00:00"
    },
    "timestamp": 1715318400000
}
```

#### PUT /api/image2text/{id} - 更新分析结果

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
    "description": "string (必填，编辑后的描述)"
}
```

**成功响应**:
```json
{
    "code": 200,
    "message": "更新成功",
    "data": {
        "id": 1,
        "description": "编辑后的描述内容..."
    },
    "timestamp": 1715318400000
}
```

## 6. 风格管理模块 API

### 5.1 接口列表

| HTTP方法 | 路径 | 描述 | 权限 |
|---------|-----|-----|-----|
| GET | /api/styles | 获取风格列表 | 已登录 |
| POST | /api/styles | 创建风格 | 设计师/管理员 |
| GET | /api/styles/{id} | 获取风格详情 | 已登录 |
| PUT | /api/styles/{id} | 更新风格 | 设计师/管理员 |
| DELETE | /api/styles/{id} | 删除风格 | 设计师/管理员 |

### 5.2 接口详情

#### POST /api/styles - 创建风格

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
    "name": "string (必填，风格名称，1-64字符)",
    "description": "string (选填，风格描述)",
    "previewUrl": "string (选填，预览图URL)",
    "config": "object (必填，风格配置JSON)"
}
```

**config字段说明**:
```json
{
    "prompt": "string (正面提示词)",
    "negativePrompt": "string (负面提示词)",
    "model": "string (使用的模型名称)",
    "params": {
        "steps": 20,
        "cfgScale": 7.5
    }
}
```

**成功响应**:
```json
{
    "code": 200,
    "message": "创建成功",
    "data": {
        "id": 1,
        "name": "我的风格",
        "description": "自定义风格描述",
        "previewUrl": "https://example.com/preview.png",
        "config": {...},
        "status": "ACTIVE",
        "createdAt": "2026-05-10T10:00:00"
    },
    "timestamp": 1715318400000
}
```

## 7. LoRA训练模块 API

### 6.1 接口列表

| HTTP方法 | 路径 | 描述 | 权限 |
|---------|-----|-----|-----|
| POST | /api/lora/tasks | 创建训练任务 | 设计师/管理员 |
| GET | /api/lora/tasks | 获取任务列表 | 设计师/管理员 |
| GET | /api/lora/tasks/{id} | 获取任务详情 | 设计师/管理员 |
| PUT | /api/lora/tasks/{id}/start | 启动训练 | 设计师/管理员 |
| GET | /api/lora/tasks/{id}/logs | 获取训练日志 | 设计师/管理员 |
| GET | /api/lora/tasks/{id}/download | 下载模型 | 设计师/管理员 |
| DELETE | /api/lora/tasks/{id} | 删除任务 | 设计师/管理员 |

### 6.2 接口详情

#### POST /api/lora/tasks - 创建训练任务

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
    "name": "string (必填，任务名称)",
    "params": {
        "epochs": "Integer (必填，训练轮数，默认10)",
        "batchSize": "Integer (必填，批次大小，默认8)",
        "learningRate": "Float (选填，学习率，默认0.0001)",
        "resolution": "Integer (选填，图像分辨率，默认512)",
        "networkDim": "Integer (选填，网络维度，默认64)",
        "networkAlpha": "Integer (选填，网络Alpha，默认32)"
    },
    "dataPath": "string (必填，训练数据路径)"
}
```

**成功响应**:
```json
{
    "code": 200,
    "message": "任务创建成功",
    "data": {
        "id": 1,
        "name": "我的LoRA训练",
        "status": "PENDING",
        "params": {...},
        "progress": 0,
        "createdAt": "2026-05-10T10:00:00"
    },
    "timestamp": 1715318400000
}
```

#### PUT /api/lora/tasks/{id}/start - 启动训练

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
    "code": 200,
    "message": "训练已启动",
    "data": {
        "id": 1,
        "status": "RUNNING",
        "progress": 0,
        "startedAt": "2026-05-10T10:00:00"
    },
    "timestamp": 1715318400000
}
```

#### GET /api/lora/tasks/{id}/logs - 获取训练日志

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "taskId": 1,
        "logs": "Epoch 1/10...\nLoss: 0.5...\nEpoch 2/10...\nLoss: 0.3..."
    },
    "timestamp": 1715318400000
}
```

## 8. 系统管理模块 API

### 7.1 接口列表

| HTTP方法 | 路径 | 描述 | 权限 |
|---------|-----|-----|-----|
| GET | /api/system/configs | 获取系统配置列表 | 管理员 |
| GET | /api/system/configs/{key} | 获取单个配置 | 管理员 |
| PUT | /api/system/configs/{key} | 更新配置 | 管理员 |
| GET | /api/system/logs | 获取系统日志 | 管理员 |
| POST | /api/system/logs/export | 导出日志 | 管理员 |
| POST | /api/system/models/upload | 上传模型文件 | 管理员 |
| GET | /api/system/models | 获取模型列表 | 管理员 |
| DELETE | /api/system/models/{id} | 删除模型 | 管理员 |

### 7.2 接口详情

#### GET /api/system/configs - 获取系统配置列表

**请求头**: `Authorization: Bearer {token}`

**成功响应**:
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "configKey": "app.name",
            "configValue": "文图互转主题设计系统",
            "description": "系统名称"
        }
    ],
    "timestamp": 1715318400000
}
```

#### PUT /api/system/configs/{key} - 更新配置

**请求头**: `Authorization: Bearer {token}`

**请求体**:
```json
{
    "configValue": "string (必填，配置值)",
    "description": "string (选填，配置说明)"
}
```

**成功响应**:
```json
{
    "code": 200,
    "message": "配置更新成功",
    "data": null,
    "timestamp": 1715318400000
}
```

---

**文档版本**: v1.0  
**创建日期**: 2026-05-10  
**适用框架**: Spring Boot 3.2.x + SpringDoc OpenAPI
