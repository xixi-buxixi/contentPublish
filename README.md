# Pulse Distro (多平台内容自动适配与分发工具)

Pulse Distro 是一个基于 Java 21 + Spring Boot 3.3.x 单体架构构建的**多平台内容自动适配与分发工具**（MVP 阶段）。它旨在帮助内容创作者“一处编写，多端分发”，并提供高保真的社交平台拟态预览。

## 🚀 核心特性

- **内容标准化**：支持 Markdown 原始输入，并高保真地将其解析为平台无关的统一数据块（Flexmark-Java 驱动）。
- **智能与降级适配**：支持使用大模型（DeepSeek-v4-flash）自动针对小红书、知乎、微信公众号、B站的风格和限制条件进行重写；如大模型异常或超时，自动无缝降级为本地模板适配。
- **实时事件推送**：基于 WebSocket (Pipeline) 实时推送异步适配与发布的进程，提供断线重连的历史事件自动追平。
- **媒体规范化与安全**：自动处理本地图片上传、去重（SHA-256）、安全过滤（防路径穿越），以及引用冲突强校验。
- **仿真样板预览**：高保真拟态 HTML 渲染引擎，允许创作者在不绑定真实账号的情况下，零门槛预览内容在各平台上的实际排版。
- **浏览器插件扩展**：预留插件注册、心跳维持与真实发布回调接口，支持未来通过 Chrome 插件进行前台发布接力。

## 🛠️ 技术栈

- **后端核心**：Java 21 / Spring Boot 3.3.x / Spring Data JPA / H2 File Database (本地存储)
- **前端页面**：Tailwind CSS v4 CDN / HTML5 / WebSocket
- **Markdown 解析**：Flexmark-Java
- **LLM 框架**：LangChain4j (OpenAI-compatible / DeepSeek)
- **构建工具**：Maven 3.x

## 📂 项目文档

为了帮助您快速熟悉项目，我们提供了以下详细文档：

1. **项目整体架构与设计决策**：请参阅 [项目架构.md](项目架构.md)
2. **完整的端到端操作说明**：请参阅 [项目使用说明.md](项目使用说明.md)

## ⚙️ 快速开始

### 1. 配置环境变量
在项目根目录下创建 `.env` 文件（可参考 `.env.example`）：
```env
SERVER_PORT=8080
APP_PUBLIC_BASE_URL=http://localhost:8080
LANGCHAIN4J_API_KEY=your_deepseek_api_key_here
LANGCHAIN4J_BASE_URL=https://api.deepseek.com
LANGCHAIN4J_MODEL_NAME=deepseek-chat
```

### 2. 启动 Spring Boot 应用
使用 Maven 命令直接运行项目：
```bash
mvn spring-boot:run
```

应用启动后，浏览器访问 `http://localhost:8080` 即可打开控制台。

### 3. 运行自动化测试
执行以下命令运行所有单元测试与 API 冒烟测试：
```bash
mvn test
```
