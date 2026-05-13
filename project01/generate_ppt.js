const pptx = require("pptxgenjs");

// 创建演示文稿
const pres = new pptx();

// 设置演示文稿属性
pres.author = "Nebula Studio Team";
pres.company = "Nebula Studio";
pres.title = "文图互转主题设计系统 - 项目汇报";
pres.subject = "项目介绍与技术汇报";

// 定义配色方案 - Ocean Gradient (符合技术项目主题)
const colors = {
  primary: "065A82",    // 深蓝
  secondary: "1C7293",  // 青色
  accent: "21295C",     // 午夜蓝
  light: "F2F2F2",      // 浅灰
  white: "FFFFFF",
  dark: "0B0F19",       // 深色背景
  textLight: "F1F5F9",  // 浅色文字
  textDark: "1E2761"    // 深色文字
};

// 定义字体
const fonts = {
  title: "Arial Black",
  body: "Calibri"
};

// ========== 幻灯片1: 封面 ==========
let slide1 = pres.addSlide();
slide1.background = { color: colors.dark };

// 装饰性矩形
slide1.addShape(pres.ShapeType.rect, {
  x: 0, y: 0, w: "100%", h: 0.1,
  fill: { color: colors.secondary }
});

// 主标题
slide1.addText("文图互转主题设计系统", {
  x: 0.5, y: 2.0, w: 9, h: 1.0,
  fontSize: 44,
  bold: true,
  color: colors.white,
  align: "center",
  fontFace: fonts.title
});

// 副标题
slide1.addText("Nebula Studio", {
  x: 0.5, y: 3.2, w: 9, h: 0.6,
  fontSize: 28,
  color: colors.secondary,
  align: "center",
  fontFace: fonts.body
});

// 项目信息
slide1.addText([
  { text: "项目汇报", options: { bold: true, color: colors.white } },
  { text: "\n\n", options: {} },
  { text: "编制日期: 2026-05-12", options: { color: colors.light } },
  { text: "\n", options: {} },
  { text: "项目进度: 70% (7/10阶段已完成)", options: { color: colors.light } }
], {
  x: 0.5, y: 4.5, w: 9, h: 1.5,
  fontSize: 16,
  align: "center",
  fontFace: fonts.body
});

// 底部装饰
slide1.addShape(pres.ShapeType.rect, {
  x: 0, y: 6.9, w: "100%", h: 0.1,
  fill: { color: colors.secondary }
});

// ========== 幻灯片2: 目录 ==========
let slide2 = pres.addSlide();
slide2.background = { color: colors.white };

// 标题
slide2.addText("目录", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

// 装饰线
slide2.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 2, h: 0.05,
  fill: { color: colors.secondary }
});

// 目录项
const tocItems = [
  { num: "01", title: "项目概述", desc: "背景、目标、核心价值" },
  { num: "02", title: "系统思考", desc: "战略意图、第一性原理、竞品分析" },
  { num: "03", title: "需求分析", desc: "用户角色、功能模块" },
  { num: "04", title: "架构设计", desc: "系统架构、技术选型" },
  { num: "05", title: "详细设计", desc: "分层架构、核心类设计" },
  { num: "06", title: "页面原型设计", desc: "设计系统、色彩体系" },
  { num: "07", title: "实际系统实现", desc: "前端、后端、测试联调" },
  { num: "08", title: "项目总结", desc: "进度、工作量、技术亮点" }
];

tocItems.forEach((item, index) => {
  const yPos = 1.8 + index * 0.6;
  
  // 编号圆圈
  slide2.addShape(pres.ShapeType.ellipse, {
    x: 0.8, y: yPos - 0.05, w: 0.4, h: 0.4,
    fill: { color: colors.secondary },
    line: { type: "none" }
  });
  
  slide2.addText(item.num, {
    x: 0.8, y: yPos - 0.05, w: 0.4, h: 0.4,
    fontSize: 14,
    bold: true,
    color: colors.white,
    align: "center",
    valign: "middle"
  });
  
  // 标题
  slide2.addText(item.title, {
    x: 1.5, y: yPos, w: 4, h: 0.3,
    fontSize: 18,
    bold: true,
    color: colors.textDark,
    valign: "middle"
  });
  
  // 描述
  slide2.addText(item.desc, {
    x: 5.5, y: yPos, w: 4, h: 0.3,
    fontSize: 14,
    color: "666666",
    valign: "middle"
  });
});

// ========== 幻灯片3: 项目概述 - 背景 ==========
let slide3 = pres.addSlide();
slide3.background = { color: colors.white };

// 标题
slide3.addText("01 项目概述", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide3.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 2, h: 0.05,
  fill: { color: colors.secondary }
});

// 背景卡片
slide3.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.8, w: 9, h: 1.5,
  fill: { color: "F8F9FA" },
  line: { color: colors.secondary, width: 2 }
});

slide3.addText("项目背景", {
  x: 0.8, y: 2.0, w: 8.4, h: 0.4,
  fontSize: 20,
  bold: true,
  color: colors.textDark
});

slide3.addText("当前AIGC领域正经历爆发式增长，但企业在实际业务应用中面临三大核心困境：", {
  x: 0.8, y: 2.5, w: 8.4, h: 0.6,
  fontSize: 14,
  color: "333333"
});

// 三个困境卡片
const problems = [
  { icon: "⚡", title: "效率瓶颈", desc: "设计师产出内容的速度远跟不上业务需求的速度" },
  { icon: "🔗", title: "一致性难题", desc: "多模态内容（图文）的一致性维护成本极高" },
  { icon: "💰", title: "定制化成本", desc: "通用AI模型难以满足企业特定风格需求" }
];

problems.forEach((item, index) => {
  const xPos = 0.8 + index * 3;
  
  slide3.addShape(pres.ShapeType.rect, {
    x: xPos, y: 3.8, w: 2.6, h: 2.5,
    fill: { color: colors.white },
    line: { color: "E0E0E0", width: 1 }
  });
  
  slide3.addText(item.icon, {
    x: xPos, y: 4.0, w: 2.6, h: 0.6,
    fontSize: 36,
    align: "center"
  });
  
  slide3.addText(item.title, {
    x: xPos, y: 4.7, w: 2.6, h: 0.4,
    fontSize: 18,
    bold: true,
    color: colors.textDark,
    align: "center"
  });
  
  slide3.addText(item.desc, {
    x: xPos + 0.2, y: 5.2, w: 2.2, h: 0.8,
    fontSize: 12,
    color: "666666",
    align: "center"
  });
});

// ========== 幻灯片4: 项目概述 - 目标与价值 ==========
let slide4 = pres.addSlide();
slide4.background = { color: colors.white };

// 标题
slide4.addText("项目目标与核心价值", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide4.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 3, h: 0.05,
  fill: { color: colors.secondary }
});

// 项目目标
slide4.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.8, w: 4.3, h: 4.5,
  fill: { color: "F8F9FA" },
  line: { color: colors.secondary, width: 2 }
});

slide4.addText("项目目标", {
  x: 0.8, y: 2.0, w: 3.7, h: 0.4,
  fontSize: 20,
  bold: true,
  color: colors.textDark
});

const goals = [
  "为企业提供一站式的多模态内容生成解决方案",
  "实现创意可复用，风格可积累的核心价值主张",
  "支持三种角色：普通用户、设计师、系统管理员"
];

goals.forEach((goal, index) => {
  slide4.addShape(pres.ShapeType.ellipse, {
    x: 0.8, y: 2.6 + index * 0.6, w: 0.2, h: 0.2,
    fill: { color: colors.secondary },
    line: { type: "none" }
  });
  
  slide4.addText(goal, {
    x: 1.2, y: 2.5 + index * 0.6, w: 3.3, h: 0.5,
    fontSize: 13,
    color: "333333",
    valign: "middle"
  });
});

// 核心价值
slide4.addShape(pres.ShapeType.rect, {
  x: 5.2, y: 1.8, w: 4.3, h: 4.5,
  fill: { color: colors.primary },
  line: { type: "none" }
});

slide4.addText("核心价值", {
  x: 5.5, y: 2.0, w: 3.7, h: 0.4,
  fontSize: 20,
  bold: true,
  color: colors.white
});

const values = [
  { user: "普通用户", value: "零门槛实现专业级内容创作，个人效率提升10倍以上" },
  { user: "设计师", value: "从重复劳动中解放，专注于创意和审美决策" },
  { user: "系统管理员", value: "统一管控内容质量，降低合规风险" },
  { user: "业务组织", value: "内容生产成本预计降低60%，交付周期缩短80%" }
];

values.forEach((item, index) => {
  slide4.addText(item.user + ":", {
    x: 5.5, y: 2.7 + index * 0.9, w: 3.7, h: 0.3,
    fontSize: 14,
    bold: true,
    color: colors.secondary
  });
  
  slide4.addText(item.value, {
    x: 5.5, y: 3.0 + index * 0.9, w: 3.7, h: 0.5,
    fontSize: 12,
    color: colors.white
  });
});

// ========== 幻灯片5: 系统思考 ==========
let slide5 = pres.addSlide();
slide5.background = { color: colors.dark };

// 标题
slide5.addText("02 系统思考", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.white,
  fontFace: fonts.title
});

slide5.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 2.5, h: 0.05,
  fill: { color: colors.secondary }
});

// 战略意图
slide5.addText("战略意图", {
  x: 0.5, y: 1.8, w: 9, h: 0.4,
  fontSize: 24,
  bold: true,
  color: colors.secondary
});

slide5.addText("成为企业内容生产的\"智能中枢\"，让创意规模化成为可能", {
  x: 0.5, y: 2.3, w: 9, h: 0.5,
  fontSize: 16,
  color: colors.white,
  italic: true
});

// 第一性原理
slide5.addText("第一性原理：创意可复用，风格可积累", {
  x: 0.5, y: 3.2, w: 9, h: 0.4,
  fontSize: 20,
  bold: true,
  color: colors.secondary
});

// 对比图
slide5.addShape(pres.ShapeType.rect, {
  x: 0.8, y: 3.8, w: 3.8, h: 2.5,
  fill: { color: "1E2761" },
  line: { color: colors.secondary, width: 1 }
});

slide5.addText("传统内容生产", {
  x: 0.8, y: 4.0, w: 3.8, h: 0.4,
  fontSize: 16,
  bold: true,
  color: colors.white,
  align: "center"
});

slide5.addText("用户A → 创作1 → 交付\n用户B → 创作2 → 交付（风格割裂）\n用户C → 创作3 → 交付（创意无法复用）", {
  x: 1.0, y: 4.5, w: 3.4, h: 1.5,
  fontSize: 12,
  color: colors.light,
  align: "center"
});

slide5.addShape(pres.ShapeType.rect, {
  x: 5.4, y: 3.8, w: 3.8, h: 2.5,
  fill: { color: colors.primary },
  line: { color: colors.secondary, width: 1 }
});

slide5.addText("基于本系统的生产", {
  x: 5.4, y: 4.0, w: 3.8, h: 0.4,
  fontSize: 16,
  bold: true,
  color: colors.white,
  align: "center"
});

slide5.addText("设计师 → 定义风格 → LoRA模型\n    ├─ 用户A → 生成1\n    ├─ 用户B → 生成2\n    └─ 用户C → 生成3\n       ↓\n  风格一致性 + 创意可复用", {
  x: 5.6, y: 4.5, w: 3.4, h: 1.5,
  fontSize: 11,
  color: colors.white
});

// ========== 幻灯片6: 竞品分析 ==========
let slide6 = pres.addSlide();
slide6.background = { color: colors.white };

// 标题
slide6.addText("竞品分析与差异化定位", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide6.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 4, h: 0.05,
  fill: { color: colors.secondary }
});

// 表格标题
slide6.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.8, w: 9, h: 0.5,
  fill: { color: colors.primary },
  line: { type: "none" }
});

slide6.addText("解决方案", {
  x: 0.5, y: 1.8, w: 2.5, h: 0.5,
  fontSize: 14,
  bold: true,
  color: colors.white,
  align: "center",
  valign: "middle"
});

slide6.addText("优势", {
  x: 3.0, y: 1.8, w: 2, h: 0.5,
  fontSize: 14,
  bold: true,
  color: colors.white,
  align: "center",
  valign: "middle"
});

slide6.addText("劣势", {
  x: 5.0, y: 1.8, w: 2, h: 0.5,
  fontSize: 14,
  bold: true,
  color: colors.white,
  align: "center",
  valign: "middle"
});

slide6.addText("差异化定位", {
  x: 7.0, y: 1.8, w: 2.5, h: 0.5,
  fontSize: 14,
  bold: true,
  color: colors.white,
  align: "center",
  valign: "middle"
});

// 表格内容
const competitors = [
  { name: "Midjourney", adv: "社区活跃、质量高", dis: "仅英文、无法私有化", diff: "中文化+私有化+风格资产" },
  { name: "Stable Diffusion", adv: "开源可定制", dis: "部署复杂、体验差", diff: "企业级体验+权限管理" },
  { name: "豆包/文心一格", adv: "大厂背书", dis: "定制化有限", diff: "LoRA训练+资产沉淀" }
];

competitors.forEach((item, index) => {
  const yPos = 2.3 + index * 0.8;
  const bgColor = index % 2 === 0 ? "F8F9FA" : colors.white;
  
  slide6.addShape(pres.ShapeType.rect, {
    x: 0.5, y: yPos, w: 9, h: 0.8,
    fill: { color: bgColor },
    line: { color: "E0E0E0", width: 1 }
  });
  
  slide6.addText(item.name, {
    x: 0.5, y: yPos, w: 2.5, h: 0.8,
    fontSize: 13,
    bold: true,
    color: colors.textDark,
    align: "center",
    valign: "middle"
  });
  
  slide6.addText(item.adv, {
    x: 3.0, y: yPos, w: 2, h: 0.8,
    fontSize: 11,
    color: "333333",
    align: "center",
    valign: "middle"
  });
  
  slide6.addText(item.dis, {
    x: 5.0, y: yPos, w: 2, h: 0.8,
    fontSize: 11,
    color: "333333",
    align: "center",
    valign: "middle"
  });
  
  slide6.addText(item.diff, {
    x: 7.0, y: yPos, w: 2.5, h: 0.8,
    fontSize: 11,
    bold: true,
    color: colors.secondary,
    align: "center",
    valign: "middle"
  });
});

// 差异化核心
slide6.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 4.8, w: 9, h: 1.0,
  fill: { color: colors.secondary },
  line: { type: "none" }
});

slide6.addText("差异化核心：不是另一个AI绘图工具，而是企业级内容生产的操作系统", {
  x: 0.5, y: 4.8, w: 9, h: 1.0,
  fontSize: 16,
  bold: true,
  color: colors.white,
  align: "center",
  valign: "middle"
});

// ========== 幻灯片7: 需求分析 - 用户角色 ==========
let slide7 = pres.addSlide();
slide7.background = { color: colors.white };

// 标题
slide7.addText("03 需求分析 - 用户角色", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide7.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 4.5, h: 0.05,
  fill: { color: colors.secondary }
});

// 三个角色卡片
const roles = [
  {
    title: "普通用户",
    desc: "系统的基础使用者",
    perms: ["文生图", "图生文", "查看历史记录", "个人设置"],
    color: "3498DB"
  },
  {
    title: "设计师",
    desc: "负责风格定义与模型训练",
    perms: ["普通用户权限", "主题风格管理", "LoRA训练管理"],
    color: "9B59B6"
  },
  {
    title: "系统管理员",
    desc: "负责系统运维与用户管理",
    perms: ["全部权限", "用户管理", "系统配置", "日志管理"],
    color: "E74C3C"
  }
];

roles.forEach((role, index) => {
  const xPos = 0.5 + index * 3.2;
  
  slide7.addShape(pres.ShapeType.rect, {
    x: xPos, y: 1.8, w: 3, h: 4.0,
    fill: { color: "F8F9FA" },
    line: { color: role.color, width: 3 }
  });
  
  slide7.addText(role.title, {
    x: xPos, y: 2.0, w: 3, h: 0.5,
    fontSize: 20,
    bold: true,
    color: role.color,
    align: "center"
  });
  
  slide7.addText(role.desc, {
    x: xPos, y: 2.6, w: 3, h: 0.4,
    fontSize: 13,
    color: "666666",
    align: "center",
    italic: true
  });
  
  slide7.addShape(pres.ShapeType.rect, {
    x: xPos + 0.2, y: 3.2, w: 2.6, h: 0.05,
    fill: { color: role.color }
  });
  
  role.perms.forEach((perm, pIndex) => {
    slide7.addText("✓ " + perm, {
      x: xPos + 0.3, y: 3.5 + pIndex * 0.5, w: 2.4, h: 0.4,
      fontSize: 12,
      color: "333333",
      valign: "middle"
    });
  });
});

// ========== 幻灯片8: 需求分析 - 功能模块 ==========
let slide8 = pres.addSlide();
slide8.background = { color: colors.white };

// 标题
slide8.addText("功能模块划分", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide8.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 2.5, h: 0.05,
  fill: { color: colors.secondary }
});

// 功能模块表格
const modules = [
  { id: "F01", name: "用户管理模块", desc: "用户注册、登录、个人信息管理" },
  { id: "F02", name: "文生图模块", desc: "文本描述生成图像" },
  { id: "F03", name: "图生文模块", desc: "图像分析生成文本描述" },
  { id: "F04", name: "主题风格管理模块", desc: "风格创建、编辑、删除、应用" },
  { id: "F05", name: "LoRA模型训练模块", desc: "训练数据上传、参数配置、训练启动" },
  { id: "F06", name: "系统管理模块", desc: "用户管理、系统配置、日志管理" }
];

modules.forEach((mod, index) => {
  const yPos = 1.8 + index * 0.75;
  
  slide8.addShape(pres.ShapeType.rect, {
    x: 0.8, y: yPos, w: 8.4, h: 0.65,
    fill: { color: index % 2 === 0 ? "F8F9FA" : colors.white },
    line: { color: "E0E0E0", width: 1 }
  });
  
  slide8.addShape(pres.ShapeType.rect, {
    x: 0.8, y: yPos, w: 1.2, h: 0.65,
    fill: { color: colors.secondary },
    line: { type: "none" }
  });
  
  slide8.addText(mod.id, {
    x: 0.8, y: yPos, w: 1.2, h: 0.65,
    fontSize: 16,
    bold: true,
    color: colors.white,
    align: "center",
    valign: "middle"
  });
  
  slide8.addText(mod.name, {
    x: 2.2, y: yPos, w: 3, h: 0.65,
    fontSize: 14,
    bold: true,
    color: colors.textDark,
    valign: "middle"
  });
  
  slide8.addText(mod.desc, {
    x: 5.3, y: yPos, w: 3.5, h: 0.65,
    fontSize: 12,
    color: "666666",
    valign: "middle"
  });
});

// ========== 幻灯片9: 架构设计 ==========
let slide9 = pres.addSlide();
slide9.background = { color: colors.white };

// 标题
slide9.addText("04 架构设计", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide9.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 2, h: 0.05,
  fill: { color: colors.secondary }
});

// 三层架构图
const layers = [
  { name: "表现层 (Presentation)", tech: "Vue 3 + Element Plus + Vite", color: "3498DB" },
  { name: "业务层 (Business)", tech: "用户服务 | 文生图服务 | 图生文服务 | 风格服务 | 训练服务", color: "2ECC71" },
  { name: "基础设施层 (Infrastructure)", tech: "MySQL 8.0+ | Redis | MinIO | Kafka | AI模型接口", color: "9B59B6" }
];

layers.forEach((layer, index) => {
  const yPos = 1.8 + index * 1.4;
  
  slide9.addShape(pres.ShapeType.rect, {
    x: 1.5, y: yPos, w: 7, h: 1.0,
    fill: { color: layer.color },
    line: { type: "none" }
  });
  
  slide9.addText(layer.name, {
    x: 1.5, y: yPos + 0.1, w: 7, h: 0.4,
    fontSize: 18,
    bold: true,
    color: colors.white,
    align: "center"
  });
  
  slide9.addText(layer.tech, {
    x: 1.5, y: yPos + 0.5, w: 7, h: 0.4,
    fontSize: 12,
    color: colors.white,
    align: "center"
  });
  
  if (index < 2) {
    slide9.addText("↓", {
      x: 4.5, y: yPos + 1.05, w: 1, h: 0.3,
      fontSize: 20,
      color: colors.textDark,
      align: "center"
    });
  }
});

// ========== 幻灯片10: 技术选型 ==========
let slide10 = pres.addSlide();
slide10.background = { color: colors.white };

// 标题
slide10.addText("技术选型", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide10.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 1.8, h: 0.05,
  fill: { color: colors.secondary }
});

// 前端技术栈
slide10.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.8, w: 4.3, h: 4.0,
  fill: { color: "EBF5FB" },
  line: { color: "3498DB", width: 2 }
});

slide10.addText("前端技术栈", {
  x: 0.7, y: 2.0, w: 3.9, h: 0.4,
  fontSize: 18,
  bold: true,
  color: "2980B9"
});

const frontendTech = [
  { name: "Vue", version: "3.4.x", reason: "响应式设计，TypeScript支持" },
  { name: "Element Plus", version: "2.x", reason: "基于Vue 3，组件丰富" },
  { name: "Pinia", version: "2.x", reason: "Vue 3推荐，TypeScript友好" },
  { name: "Vite", version: "5.x", reason: "快速构建，热更新" }
];

frontendTech.forEach((tech, index) => {
  slide10.addText(tech.name + " " + tech.version, {
    x: 0.7, y: 2.6 + index * 0.7, w: 3.9, h: 0.3,
    fontSize: 14,
    bold: true,
    color: colors.textDark
  });
  
  slide10.addText(tech.reason, {
    x: 0.7, y: 2.9 + index * 0.7, w: 3.9, h: 0.3,
    fontSize: 11,
    color: "666666"
  });
});

// 后端技术栈
slide10.addShape(pres.ShapeType.rect, {
  x: 5.2, y: 1.8, w: 4.3, h: 4.0,
  fill: { color: "E8F8F5" },
  line: { color: "2ECC71", width: 2 }
});

slide10.addText("后端技术栈", {
  x: 5.4, y: 2.0, w: 3.9, h: 0.4,
  fontSize: 18,
  bold: true,
  color: "27AE60"
});

const backendTech = [
  { name: "Spring Boot", version: "3.2.x", reason: "社区成熟，生态完善" },
  { name: "MyBatis-Plus", version: "3.5.x", reason: "简化数据库操作" },
  { name: "MySQL", version: "8.0+", reason: "生产环境首选" },
  { name: "Redis", version: "7.x", reason: "高性能键值存储" },
  { name: "JWT", version: "0.12.x", reason: "无状态认证" }
];

backendTech.forEach((tech, index) => {
  slide10.addText(tech.name + " " + tech.version, {
    x: 5.4, y: 2.6 + index * 0.6, w: 3.9, h: 0.3,
    fontSize: 13,
    bold: true,
    color: colors.textDark
  });
  
  slide10.addText(tech.reason, {
    x: 5.4, y: 2.9 + index * 0.6, w: 3.9, h: 0.25,
    fontSize: 10,
    color: "666666"
  });
});

// ========== 幻灯片11: 详细设计 ==========
let slide11 = pres.addSlide();
slide11.background = { color: colors.white };

// 标题
slide11.addText("05 详细设计", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide11.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 2, h: 0.05,
  fill: { color: colors.secondary }
});

// 分层架构
slide11.addText("分层架构设计", {
  x: 0.5, y: 1.8, w: 9, h: 0.4,
  fontSize: 20,
  bold: true,
  color: colors.textDark
});

const archLayers = [
  { name: "表现层", duty: "处理HTTP请求/响应，参数校验", comp: "Controller、DTO" },
  { name: "业务层", duty: "实现业务逻辑，事务管理", comp: "Service、Strategy" },
  { name: "数据访问层", duty: "数据库CRUD操作，实体映射", comp: "Repository、Entity" },
  { name: "基础设施层", duty: "提供基础服务", comp: "Security、Redis、MinIO" }
];

archLayers.forEach((layer, index) => {
  const yPos = 2.4 + index * 0.9;
  
  slide11.addShape(pres.ShapeType.rect, {
    x: 0.8, y: yPos, w: 8.4, h: 0.75,
    fill: { color: index % 2 === 0 ? "F8F9FA" : colors.white },
    line: { color: colors.secondary, width: 1 }
  });
  
  slide11.addShape(pres.ShapeType.rect, {
    x: 0.8, y: yPos, w: 1.5, h: 0.75,
    fill: { color: colors.secondary },
    line: { type: "none" }
  });
  
  slide11.addText(layer.name, {
    x: 0.8, y: yPos, w: 1.5, h: 0.75,
    fontSize: 14,
    bold: true,
    color: colors.white,
    align: "center",
    valign: "middle"
  });
  
  slide11.addText(layer.duty, {
    x: 2.5, y: yPos + 0.05, w: 3.5, h: 0.35,
    fontSize: 12,
    color: colors.textDark,
    valign: "middle"
  });
  
  slide11.addText("主要组件: " + layer.comp, {
    x: 2.5, y: yPos + 0.4, w: 3.5, h: 0.3,
    fontSize: 11,
    color: "666666",
    valign: "middle"
  });
});

// ========== 幻灯片12: 设计模式 ==========
let slide12 = pres.addSlide();
slide12.background = { color: colors.white };

// 标题
slide12.addText("设计模式应用 - 策略模式", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide12.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 4, h: 0.05,
  fill: { color: colors.secondary }
});

// 应用场景
slide12.addShape(pres.ShapeType.rect, {
  x: 0.8, y: 1.8, w: 8.4, h: 0.6,
  fill: { color: "EBF5FB" },
  line: { color: "3498DB", width: 2 }
});

slide12.addText("应用场景：支持多种AI模型（Stable Diffusion、豆包等）", {
  x: 1.0, y: 1.8, w: 8, h: 0.6,
  fontSize: 14,
  color: "2980B9",
  valign: "middle"
});

// 策略模式图示
slide12.addText("AIClient (接口)", {
  x: 3.5, y: 2.8, w: 3, h: 0.5,
  fontSize: 16,
  bold: true,
  color: colors.textDark,
  align: "center"
});

slide12.addShape(pres.ShapeType.rect, {
  x: 3.5, y: 3.3, w: 3, h: 0.8,
  fill: { color: "F8F9FA" },
  line: { color: colors.secondary, width: 2 }
});

slide12.addText("+ generateImage(prompt, params)\n+ getName()", {
  x: 3.5, y: 3.3, w: 3, h: 0.8,
  fontSize: 11,
  color: "333333",
  align: "center",
  valign: "middle"
});

// 两个实现类
slide12.addShape(pres.ShapeType.rect, {
  x: 1.0, y: 4.5, w: 3, h: 1.0,
  fill: { color: "E8F8F5" },
  line: { color: "2ECC71", width: 2 }
});

slide12.addText("StableDiffusionClient", {
  x: 1.0, y: 4.6, w: 3, h: 0.3,
  fontSize: 13,
  bold: true,
  color: "27AE60",
  align: "center"
});

slide12.addText("实现 generateImage()", {
  x: 1.0, y: 4.9, w: 3, h: 0.5,
  fontSize: 11,
  color: "333333",
  align: "center"
});

slide12.addShape(pres.ShapeType.rect, {
  x: 6.0, y: 4.5, w: 3, h: 1.0,
  fill: { color: "FEF9E7" },
  line: { color: "F39C12", width: 2 }
});

slide12.addText("DoubaoClient", {
  x: 6.0, y: 4.6, w: 3, h: 0.3,
  fontSize: 13,
  bold: true,
  color: "E67E22",
  align: "center"
});

slide12.addText("实现 generateImage()", {
  x: 6.0, y: 4.9, w: 3, h: 0.5,
  fontSize: 11,
  color: "333333",
  align: "center"
});

// 设计理由
slide12.addShape(pres.ShapeType.rect, {
  x: 0.8, y: 5.8, w: 8.4, h: 0.8,
  fill: { color: colors.secondary },
  line: { type: "none" }
});

slide12.addText("设计理由：将不同AI模型的调用逻辑封装为独立策略，通过工厂模式动态选择，便于扩展新模型，符合开闭原则", {
  x: 1.0, y: 5.8, w: 8, h: 0.8,
  fontSize: 12,
  color: colors.white,
  valign: "middle"
});

// ========== 幻灯片13: 页面原型设计 ==========
let slide13 = pres.addSlide();
slide13.background = { color: colors.white };

// 标题
slide13.addText("06 页面原型设计", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide13.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 2.5, h: 0.05,
  fill: { color: colors.secondary }
});

// 设计风格
slide13.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.8, w: 9, h: 0.6,
  fill: { color: colors.dark },
  line: { type: "none" }
});

slide13.addText("设计风格：Nebula Studio — 深色创意工具美学，灵感源自星云与数字创作空间的融合", {
  x: 0.7, y: 1.8, w: 8.6, h: 0.6,
  fontSize: 13,
  color: colors.white,
  valign: "middle"
});

// 色彩体系
slide13.addText("色彩体系", {
  x: 0.5, y: 2.7, w: 9, h: 0.4,
  fontSize: 20,
  bold: true,
  color: colors.textDark
});

const colorScheme = [
  { name: "主色", value: "#6366f1", desc: "品牌标识、主要操作按钮" },
  { name: "强调色", value: "#22d3ee", desc: "辅助高亮、数据指标" },
  { name: "背景深", value: "#0b0f19", desc: "页面底色" },
  { name: "背景表面", value: "#141824", desc: "卡片、面板" },
  { name: "文字主", value: "#f1f5f9", desc: "标题、正文" }
];

colorScheme.forEach((color, index) => {
  const xPos = 0.8 + (index % 3) * 3;
  const yPos = 3.3 + Math.floor(index / 3) * 1.3;
  
  slide13.addShape(pres.ShapeType.rect, {
    x: xPos, y: yPos, w: 2.6, h: 1.0,
    fill: { color: "F8F9FA" },
    line: { color: "E0E0E0", width: 1 }
  });
  
  slide13.addShape(pres.ShapeType.rect, {
    x: xPos + 0.2, y: yPos + 0.2, w: 0.6, h: 0.6,
    fill: { color: color.value.replace("#", "") },
    line: { type: "none" }
  });
  
  slide13.addText(color.name, {
    x: xPos + 0.9, y: yPos + 0.2, w: 1.5, h: 0.3,
    fontSize: 13,
    bold: true,
    color: colors.textDark
  });
  
  slide13.addText(color.value, {
    x: xPos + 0.9, y: yPos + 0.5, w: 1.5, h: 0.3,
    fontSize: 10,
    color: "666666"
  });
  
  slide13.addText(color.desc, {
    x: xPos + 0.2, y: yPos + 0.85, w: 2.2, h: 0.15,
    fontSize: 9,
    color: "999999"
  });
});

// ========== 幻灯片14: 实际系统实现 - 前端 ==========
let slide14 = pres.addSlide();
slide14.background = { color: colors.white };

// 标题
slide14.addText("07 实际系统实现 - 前端", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide14.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 4, h: 0.05,
  fill: { color: colors.secondary }
});

// 前端技术栈
const frontendStack = [
  { tech: "Vue", version: "3.4.x", usage: "响应式框架" },
  { tech: "Vite", version: "5.x", usage: "构建工具" },
  { tech: "Element Plus", version: "2.7.x", usage: "UI组件库" },
  { tech: "Pinia", version: "2.x", usage: "状态管理" },
  { tech: "Vue Router", version: "4.x", usage: "路由管理" },
  { tech: "Axios", version: "1.6.x", usage: "HTTP客户端" }
];

slide14.addText("技术栈", {
  x: 0.5, y: 1.8, w: 4.3, h: 0.4,
  fontSize: 18,
  bold: true,
  color: colors.textDark
});

frontendStack.forEach((item, index) => {
  const yPos = 2.3 + index * 0.6;
  
  slide14.addShape(pres.ShapeType.rect, {
    x: 0.7, y: yPos, w: 1.2, h: 0.4,
    fill: { color: colors.secondary },
    line: { type: "none" }
  });
  
  slide14.addText(item.tech + " " + item.version, {
    x: 0.7, y: yPos, w: 1.2, h: 0.4,
    fontSize: 10,
    bold: true,
    color: colors.white,
    align: "center",
    valign: "middle"
  });
  
  slide14.addText(item.usage, {
    x: 2.0, y: yPos, w: 2.5, h: 0.4,
    fontSize: 12,
    color: "333333",
    valign: "middle"
  });
});

// 实现亮点
slide14.addShape(pres.ShapeType.rect, {
  x: 5.2, y: 1.8, w: 4.3, h: 4.0,
  fill: { color: "F8F9FA" },
  line: { color: colors.secondary, width: 2 }
});

slide14.addText("实现亮点", {
  x: 5.4, y: 2.0, w: 3.9, h: 0.4,
  fontSize: 18,
  bold: true,
  color: colors.textDark
});

const highlights = [
  "统一请求层封装：Token自动注入、错误统一处理",
  "权限守卫：路由层面的权限控制",
  "响应式设计：支持桌面端、平板、移动端",
  "深色主题：符合创意工具美学"
];

highlights.forEach((highlight, index) => {
  slide14.addShape(pres.ShapeType.ellipse, {
    x: 5.5, y: 2.6 + index * 0.8, w: 0.2, h: 0.2,
    fill: { color: colors.secondary },
    line: { type: "none" }
  });
  
  slide14.addText(highlight, {
    x: 5.9, y: 2.5 + index * 0.8, w: 3.4, h: 0.6,
    fontSize: 12,
    color: "333333",
    valign: "middle"
  });
});

// ========== 幻灯片15: 实际系统实现 - 后端 ==========
let slide15 = pres.addSlide();
slide15.background = { color: colors.white };

// 标题
slide15.addText("07 实际系统实现 - 后端", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide15.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 4, h: 0.05,
  fill: { color: colors.secondary }
});

// 后端技术栈
const backendStack = [
  { tech: "Spring Boot", version: "3.2.5", usage: "应用框架" },
  { tech: "Java", version: "17", usage: "编程语言" },
  { tech: "MyBatis-Plus", version: "3.5.6", usage: "ORM框架" },
  { tech: "Spring Security", version: "3.2.5", usage: "安全框架" },
  { tech: "JWT", version: "0.12.5", usage: "认证机制" }
];

slide15.addText("技术栈", {
  x: 0.5, y: 1.8, w: 4.3, h: 0.4,
  fontSize: 18,
  bold: true,
  color: colors.textDark
});

backendStack.forEach((item, index) => {
  const yPos = 2.3 + index * 0.6;
  
  slide15.addShape(pres.ShapeType.rect, {
    x: 0.7, y: yPos, w: 1.5, h: 0.4,
    fill: { color: colors.primary },
    line: { type: "none" }
  });
  
  slide15.addText(item.tech + " " + item.version, {
    x: 0.7, y: yPos, w: 1.5, h: 0.4,
    fontSize: 9,
    bold: true,
    color: colors.white,
    align: "center",
    valign: "middle"
  });
  
  slide15.addText(item.usage, {
    x: 2.3, y: yPos, w: 2.2, h: 0.4,
    fontSize: 12,
    color: "333333",
    valign: "middle"
  });
});

// API端点统计
slide15.addShape(pres.ShapeType.rect, {
  x: 5.2, y: 1.8, w: 4.3, h: 4.0,
  fill: { color: "E8F8F5" },
  line: { color: "2ECC71", width: 2 }
});

slide15.addText("API端点统计", {
  x: 5.4, y: 2.0, w: 3.9, h: 0.4,
  fontSize: 18,
  bold: true,
  color: "27AE60"
});

const apiStats = [
  { module: "认证", count: "3个端点", perms: "匿名/已登录" },
  { module: "用户", count: "3个端点", perms: "已登录" },
  { module: "文生图", count: "2个端点", perms: "已登录" },
  { module: "图生文", count: "2个端点", perms: "已登录" },
  { module: "风格管理", count: "4个端点", perms: "DESIGNER/ADMIN" },
  { module: "LoRA训练", count: "6个端点", perms: "DESIGNER/ADMIN" },
  { module: "系统管理", count: "5个端点", perms: "ADMIN" }
];

apiStats.forEach((stat, index) => {
  slide15.addText(stat.module + ": " + stat.count, {
    x: 5.5, y: 2.6 + index * 0.45, w: 3.7, h: 0.3,
    fontSize: 12,
    bold: true,
    color: colors.textDark
  });
  
  slide15.addText("权限: " + stat.perms, {
    x: 5.5, y: 2.9 + index * 0.45, w: 3.7, h: 0.25,
    fontSize: 10,
    color: "666666"
  });
});

// ========== 幻灯片16: 测试联调 ==========
let slide16 = pres.addSlide();
slide16.background = { color: colors.white };

// 标题
slide16.addText("测试联调结果", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide16.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 2.5, h: 0.05,
  fill: { color: colors.secondary }
});

// 测试结果
const testResults = [
  { item: "用户登录", status: "✅ 通过", desc: "JWT Token正常生成" },
  { item: "用户注册", status: "✅ 通过", desc: "用户信息正确保存" },
  { item: "文生图", status: "✅ 通过", desc: "成功调用AI模型生成图像" },
  { item: "图生文", status: "✅ 通过", desc: "图像上传和分析正常" },
  { item: "风格管理", status: "✅ 通过", desc: "CRUD操作正常" },
  { item: "LoRA训练", status: "✅ 通过", desc: "任务创建和启动成功" },
  { item: "用户管理", status: "✅ 通过", desc: "管理员功能正常" },
  { item: "系统配置", status: "✅ 通过", desc: "配置读写正常" }
];

testResults.forEach((result, index) => {
  const yPos = 1.8 + index * 0.6;
  
  slide16.addShape(pres.ShapeType.rect, {
    x: 0.8, y: yPos, w: 8.4, h: 0.5,
    fill: { color: index % 2 === 0 ? "F8F9FA" : colors.white },
    line: { color: "E0E0E0", width: 1 }
  });
  
  slide16.addText(result.item, {
    x: 1.0, y: yPos, w: 2, h: 0.5,
    fontSize: 14,
    bold: true,
    color: colors.textDark,
    valign: "middle"
  });
  
  slide16.addText(result.status, {
    x: 3.5, y: yPos, w: 1.5, h: 0.5,
    fontSize: 14,
    bold: true,
    color: "2ECC71",
    valign: "middle"
  });
  
  slide16.addText(result.desc, {
    x: 5.2, y: yPos, w: 3.8, h: 0.5,
    fontSize: 12,
    color: "666666",
    valign: "middle"
  });
});

// ========== 幻灯片17: 项目总结 - 进度 ==========
let slide17 = pres.addSlide();
slide17.background = { color: colors.white };

// 标题
slide17.addText("08 项目总结 - 进度与工作量", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide17.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 4.5, h: 0.05,
  fill: { color: colors.secondary }
});

// 项目进度
slide17.addText("项目进度：70% (7/10阶段已完成)", {
  x: 0.5, y: 1.8, w: 9, h: 0.5,
  fontSize: 20,
  bold: true,
  color: colors.secondary
});

const phases = [
  { name: "00-系统思考", status: "✅", date: "2026-05-10" },
  { name: "01-需求分析", status: "✅", date: "2026-05-10" },
  { name: "02-架构设计", status: "✅", date: "2026-05-10" },
  { name: "03-详细设计", status: "✅", date: "2026-05-10" },
  { name: "04-页面原型", status: "✅", date: "2026-05-10" },
  { name: "05-前端实现", status: "✅", date: "2026-05-10" },
  { name: "06-后端实现", status: "✅", date: "2026-05-11" },
  { name: "07-测试联调", status: "✅", date: "2026-05-12" },
  { name: "08-部署方案", status: "⏳", date: "待执行" },
  { name: "09-运维方案", status: "⏳", date: "待执行" }
];

phases.forEach((phase, index) => {
  const yPos = 2.5 + index * 0.42;
  
  slide17.addShape(pres.ShapeType.rect, {
    x: 0.8, y: yPos, w: 8.4, h: 0.35,
    fill: { color: phase.status === "✅" ? "E8F8F5" : "FEF9E7" },
    line: { color: "E0E0E0", width: 1 }
  });
  
  slide17.addText(phase.status + " " + phase.name, {
    x: 1.0, y: yPos, w: 5, h: 0.35,
    fontSize: 12,
    color: colors.textDark,
    valign: "middle"
  });
  
  slide17.addText(phase.date, {
    x: 6.5, y: yPos, w: 2.5, h: 0.35,
    fontSize: 11,
    color: "666666",
    align: "right",
    valign: "middle"
  });
});

// ========== 幻灯片18: 工作量统计 ==========
let slide18 = pres.addSlide();
slide18.background = { color: colors.white };

// 标题
slide18.addText("已完成工作量统计", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide18.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 3.5, h: 0.05,
  fill: { color: colors.secondary }
});

// 工作量卡片
const workload = [
  { icon: "📄", category: "文档", count: "15+ 份", desc: "系统思考、需求、架构、设计、原型" },
  { icon: "🎨", category: "前端页面", count: "15 个", desc: "Vue 3 组件" },
  { icon: "⚙️", category: "后端API", count: "30+ 个", desc: "REST Controller" },
  { icon: "🗄️", category: "数据表", count: "7 张", desc: "MySQL + H2" },
  { icon: "💻", category: "代码文件", count: "100+ 个", desc: "Java + Vue" }
];

workload.forEach((item, index) => {
  const xPos = 0.8 + (index % 3) * 3;
  const yPos = 2.0 + Math.floor(index / 3) * 2.2;
  const cardWidth = index < 3 ? 2.6 : 4.0;
  const xOffset = index < 3 ? 0 : (index - 3) * 4.2;
  
  slide18.addShape(pres.ShapeType.rect, {
    x: 0.8 + xOffset, y: yPos, w: cardWidth, h: 1.8,
    fill: { color: "F8F9FA" },
    line: { color: colors.secondary, width: 2 }
  });
  
  slide18.addText(item.icon, {
    x: 0.8 + xOffset, y: yPos + 0.2, w: cardWidth, h: 0.5,
    fontSize: 36,
    align: "center"
  });
  
  slide18.addText(item.count, {
    x: 0.8 + xOffset, y: yPos + 0.8, w: cardWidth, h: 0.4,
    fontSize: 24,
    bold: true,
    color: colors.secondary,
    align: "center"
  });
  
  slide18.addText(item.category, {
    x: 0.8 + xOffset, y: yPos + 1.2, w: cardWidth, h: 0.3,
    fontSize: 14,
    bold: true,
    color: colors.textDark,
    align: "center"
  });
  
  slide18.addText(item.desc, {
    x: 0.8 + xOffset, y: yPos + 1.5, w: cardWidth, h: 0.25,
    fontSize: 10,
    color: "666666",
    align: "center"
  });
});

// ========== 幻灯片19: 技术亮点 ==========
let slide19 = pres.addSlide();
slide19.background = { color: colors.dark };

// 标题
slide19.addText("技术亮点", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.white,
  fontFace: fonts.title
});

slide19.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 1.8, h: 0.05,
  fill: { color: colors.secondary }
});

// 技术亮点
const techHighlights = [
  { 
    title: "前后端分离", 
    desc: "Vue 3 + Spring Boot，独立开发部署",
    icon: "🔄"
  },
  { 
    title: "响应式设计", 
    desc: "适配桌面端、平板、移动端",
    icon: "📱"
  },
  { 
    title: "权限精细控制", 
    desc: "RBAC + 方法级权限校验",
    icon: "🔐"
  },
  { 
    title: "统一规范", 
    desc: "响应格式、异常处理、日志记录",
    icon: "📋"
  },
  { 
    title: "可扩展架构", 
    desc: "策略模式支持多种AI模型",
    icon: "🚀"
  }
];

techHighlights.forEach((item, index) => {
  const yPos = 1.8 + index * 0.9;
  
  slide19.addShape(pres.ShapeType.rect, {
    x: 0.8, y: yPos, w: 8.4, h: 0.75,
    fill: { color: "1E2761" },
    line: { color: colors.secondary, width: 1 }
  });
  
  slide19.addText(item.icon + " " + item.title, {
    x: 1.0, y: yPos + 0.1, w: 3, h: 0.3,
    fontSize: 18,
    bold: true,
    color: colors.secondary
  });
  
  slide19.addText(item.desc, {
    x: 1.0, y: yPos + 0.4, w: 8, h: 0.3,
    fontSize: 14,
    color: colors.white
  });
});

// ========== 幻灯片20: 下一步计划 ==========
let slide20 = pres.addSlide();
slide20.background = { color: colors.white };

// 标题
slide20.addText("下一步计划", {
  x: 0.5, y: 0.5, w: 9, h: 0.6,
  fontSize: 36,
  bold: true,
  color: colors.textDark,
  fontFace: fonts.title
});

slide20.addShape(pres.ShapeType.rect, {
  x: 0.5, y: 1.2, w: 2, h: 0.05,
  fill: { color: colors.secondary }
});

// 下一步计划
const nextSteps = [
  {
    phase: "08-部署方案",
    items: [
      "Docker容器化",
      "Kubernetes编排",
      "CI/CD流水线"
    ],
    color: "3498DB"
  },
  {
    phase: "09-运维方案",
    items: [
      "监控体系",
      "日志收集",
      "告警策略",
      "备份恢复"
    ],
    color: "2ECC71"
  }
];

nextSteps.forEach((step, index) => {
  const xPos = 0.8 + index * 4.5;
  
  slide20.addShape(pres.ShapeType.rect, {
    x: xPos, y: 1.8, w: 4, h: 4.0,
    fill: { color: "F8F9FA" },
    line: { color: step.color, width: 3 }
  });
  
  slide20.addText(step.phase, {
    x: xPos, y: 2.0, w: 4, h: 0.5,
    fontSize: 20,
    bold: true,
    color: step.color,
    align: "center"
  });
  
  slide20.addShape(pres.ShapeType.rect, {
    x: xPos + 0.3, y: 2.7, w: 3.4, h: 0.05,
    fill: { color: step.color }
  });
  
  step.items.forEach((item, itemIndex) => {
    slide20.addShape(pres.ShapeType.ellipse, {
      x: xPos + 0.4, y: 3.0 + itemIndex * 0.7, w: 0.2, h: 0.2,
      fill: { color: step.color },
      line: { type: "none" }
    });
    
    slide20.addText(item, {
      x: xPos + 0.7, y: 3.0 + itemIndex * 0.7, w: 3.1, h: 0.2,
      fontSize: 14,
      color: colors.textDark,
      valign: "middle"
    });
  });
});

// ========== 幻灯片21: 结束页 ==========
let slide21 = pres.addSlide();
slide21.background = { color: colors.dark };

// 装饰性矩形
slide21.addShape(pres.ShapeType.rect, {
  x: 0, y: 0, w: "100%", h: 0.1,
  fill: { color: colors.secondary }
});

// 感谢语
slide21.addText("感谢聆听", {
  x: 0.5, y: 2.5, w: 9, h: 1.0,
  fontSize: 48,
  bold: true,
  color: colors.white,
  align: "center",
  fontFace: fonts.title
});

// 副标题
slide21.addText("Nebula Studio - 文图互转主题设计系统", {
  x: 0.5, y: 3.8, w: 9, h: 0.6,
  fontSize: 24,
  color: colors.secondary,
  align: "center",
  fontFace: fonts.body
});

// 联系信息
slide21.addText([
  { text: "项目汇报完毕\n\n", options: { fontSize: 16, color: colors.light, align: "center" } },
  { text: "编制日期: 2026-05-12", options: { fontSize: 14, color: colors.light, align: "center" } }
], {
  x: 0.5, y: 5.0, w: 9, h: 1.0,
  align: "center"
});

// 底部装饰
slide21.addShape(pres.ShapeType.rect, {
  x: 0, y: 6.9, w: "100%", h: 0.1,
  fill: { color: colors.secondary }
});

// 保存演示文稿
pres.writeFile({ fileName: "文图互转主题设计系统-项目汇报.pptx" });

console.log("✅ PPT生成成功：文图互转主题设计系统-项目汇报.pptx");
