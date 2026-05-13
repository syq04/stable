# 文图互转主题设计系统 - 页面原型设计规范

## 设计系统概述

### 设计风格
**Nebula Studio** — 深色创意工具美学，灵感源自星云与数字创作空间的融合。以 Indigo-Cyan 渐变为主色调，营造沉浸式创作氛围。

### 色彩体系

| 用途 | 色值 | 说明 |
|:---|:---|:---|
| 主色 | `#6366f1` (Indigo) | 品牌标识、主要操作按钮 |
| 主色浅 | `#818cf8` | 高亮状态、活跃导航 |
| 强调色 | `#22d3ee` (Cyan) | 辅助高亮、数据指标 |
| 成功 | `#34d399` | 完成状态、正向反馈 |
| 警告 | `#fbbf24` | 提醒、中等风险 |
| 危险 | `#f87171` | 错误、删除操作 |
| 背景深 | `#0b0f19` | 页面底色 |
| 背景表面 | `#141824` | 卡片、面板 |
| 背景提升 | `#1c2133` | 输入框、嵌套区域 |
| 文字主 | `#f1f5f9` | 标题、正文 |
| 文字次 | `#94a3b8` | 描述、辅助信息 |
| 文字弱 | `#64748b` | 占位符、禁用态 |
| 边框 | `#2a3045` | 分隔线、卡片边框 |

### 字体

| 用途 | 字体 | 回退 |
|:---|:---|:---|
| 标题/品牌 | Sora | sans-serif |
| 正文/界面 | Noto Sans SC | sans-serif |
| 代码/数据 | Courier New | monospace |

### 间距与圆角

| 变量 | 值 | 用途 |
|:---|:---|:---|
| `--radius` | 12px | 卡片、面板 |
| `--radius-sm` | 8px | 按钮、输入框、标签 |
| `--sidebar-w` | 260px | 侧边栏宽度 |
| `--header-h` | 64px | 顶栏高度 |

### 响应式断点

| 断点 | 宽度 | 适配 |
|:---|:---|:---|
| 移动端 | ≤768px | 侧边栏抽屉、单列布局 |
| 平板 | 769-1024px | 双栏调整 |
| 桌面 | ≥1025px | 完整布局 |

## 组件规范

### 侧边栏
- 固定定位，深色渐变背景 `#0e1322 → #111729`
- 导航项：14px图标 + 13px文字，hover 变亮，active 左侧3px主色条
- 底部用户信息区：34px圆角头像 + 姓名/角色

### 顶栏
- 64px高度，毛玻璃效果 `backdrop-filter: blur(16px)`
- 左侧：移动端汉堡菜单 + 页面标题
- 右侧：功能按钮组

### 卡片
- `bg-surface` 背景 + `border-color` 边框
- hover 时边框变亮 + 上移2px + 阴影
- 圆角12px

### 按钮

| 类型 | 样式 |
|:---|:---|
| 主要 | Indigo→Purple渐变，白色文字 |
| 次要 | 透明背景，边框 |
| 危险 | 红色文字，红色边框 |
| 强调 | Cyan半透明背景 |

### 表单
- 输入框：44px高度，`bg-elevated` 背景
- focus 时主色边框 + 3px glow 阴影
- 标签13px，提示文字11px

### 状态标签
- 圆角6px，11-12px字号
- 成功/警告/错误/信息各对应色系半透明背景

### 表格
- 深色表头 `bg-elevated`
- 行hover 微亮
- 操作按钮紧凑排列

## 页面清单 (14个)

| 模块 | 页面 | 文件 |
|:---|:---|:---|
| 用户管理 | 工作台 | dashboard.html |
| 用户管理 | 用户注册 | user_register.html |
| 用户管理 | 用户登录 | user_login.html |
| 用户管理 | 修改密码 | user_password.html |
| 用户管理 | 用户管理(管理员) | admin_users.html |
| 文生图 | 文生图创作 | text2image.html |
| 文生图 | 文生图历史 | text2image_history.html |
| 图生文 | 图生文分析 | image2text.html |
| 图生文 | 图生文历史 | image2text_history.html |
| 风格管理 | 风格管理 | style_management.html |
| LoRA训练 | LoRA训练 | lora_training.html |
| 系统管理 | 系统概览 | admin_dashboard.html |
| 系统管理 | 系统配置 | admin_settings.html |
| 系统管理 | 日志管理 | admin_logs.html |
| 系统管理 | 模型管理 | admin_models.html |

## 技术栈

| 技术 | 版本 | 用途 |
|:---|:---|:---|
| Vue 3 | 3.4.x | 响应式框架 |
| Element Plus | 2.7.x | UI组件库(消息提示、确认框) |
| Font Awesome | 6.5.x | 图标库 |
| Google Fonts | - | Sora + Noto Sans SC |

## 交互规范

- 所有删除操作需二次确认
- 操作成功/失败使用 Element Plus Message 提示
- 表单提交前校验必填项
- 加载状态使用旋转动画
- 页面切换使用淡入动画
