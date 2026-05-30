# Pulse Distro Agent Overview

本文件是 Codex 进入项目时优先读取的总览导航。它面向项目构建和模块协作，不替代用户当前指令。

## 1. Orchestrator 与 OverviewAgent 的边界

- **Orchestrator**：当前 Codex 的总览协调者角色。负责读取根文档、实践方案和接口文档，提取模块最小上下文，更新 `agentsPrompt/<ModuleAgent>/agent.md`，运行 Hook，并按依赖顺序推进构建。
- **OverviewAgent**：Java 业务模块。只负责 `/api/session/init`、`/ws/pipeline`、WebSocket 会话隔离、事件包装和按 `userToken` 定向推送。
- Orchestrator 可以维护所有模块 `agent.md`；Java `OverviewAgent` 不得吸收 Task、Media、Adapt、Publish 或 Plugin 的业务逻辑。

## 2. 项目描述

Pulse Distro 是一个多平台内容自动适配与分发工具。MVP 目标是在单体 Java 应用中完成：

- 输入一份原始内容。
- 生成适合小红书、知乎、微信公众号、B 站的发布版本。
- 支持多平台预览和人工编辑。
- 通过一键 `mock` 模拟发布生成发布记录和拟态页面。
- 为后续 `real` 浏览器插件发布保留接口，但不让真实发布阻塞 MVP。

推荐实现形态：

- Java 21 + Spring Boot 3.3.x 单体应用。
- H2 文件数据库。
- 本地媒体存储：`data/media/{taskId}/`。
- 静态前端打包进 Spring Boot `static`。
- WebSocket 用于适配、发布、插件状态的增量推送。
- LangChain4j 用于智能改写；缺少模型配置或超时时必须降级到模板适配。

## 3. 全局配置与隐私变量

- 根目录真实 `.env` 存放本地私密配置，必须加入 `.gitignore`。
- `.env.example` 是可提交模板，不包含真实密钥。
- Spring Boot 配置通过 `${VAR_NAME}` 引用变量，并使用 `spring.config.import=optional:file:.env[.properties]` 读取根目录 `.env`。
- Python Hook 通过 `hooks/base_hook.py` 读取根目录 `.env`，用于 Hook 输出控制和未来脚本配置。
- 建议变量：
  - `SERVER_PORT`
  - `WS_ENDPOINT`
  - `H2_DB_PATH`
  - `H2_DB_USERNAME`
  - `H2_DB_PASSWORD`
  - `MEDIA_STORAGE_ROOT`
  - `APP_PUBLIC_BASE_URL`
  - `LANGCHAIN4J_API_KEY`
  - `LANGCHAIN4J_BASE_URL`

## 4. 模块导航

| 模块 | 入口 | 主要职责 | Hook |
| --- | --- | --- | --- |
| TaskAgent | `agentsPrompt/TaskAgent/agent.md` | 内容任务、标准内容模型、Markdown 解析 | `rtk python hooks/task_hook.py` |
| MediaAgent | `agentsPrompt/MediaAgent/agent.md` | 媒体上传、本地存储、二进制访问、引用删除 | `rtk python hooks/media_hook.py` |
| ConfigAgent | `agentsPrompt/ConfigAgent/agent.md` | 平台规则配置、配置加载、规则查询 | `rtk python hooks/config_hook.py` |
| AdaptAgent | `agentsPrompt/AdaptAgent/agent.md` | 异步平台适配、LLM/模板降级、适配记录 | `rtk python hooks/adapt_hook.py` |
| PublishAgent | `agentsPrompt/PublishAgent/agent.md` | 一键发布、MockPublisher、拟态页面 | `rtk python hooks/publish_hook.py` |
| OverviewAgent | `agentsPrompt/OverviewAgent/agent.md` | 会话初始化、WebSocket、全局事件路由 | `rtk python hooks/overview_hook.py` |
| PluginAgent | `agentsPrompt/PluginAgent/agent.md` | 插件注册、心跳、真实发布回调 | `rtk python hooks/plugin_hook.py` |

## 5. 推荐源码结构

```text
src/main/java/com/example/pulsedistro
  PulseDistroApplication.java
  config/
  controller/
  domain/
  dto/
  model/
  repository/
  service/
  adapter/
  publisher/
  websocket/
  event/
src/main/resources
  application.yml
  platforms/
  static/
src/test/java/com/example/pulsedistro
```

## 6. 核心数据模型

- `ContentTask`：任务标题、输入类型、原始内容、标准内容 JSON、封面媒体、状态、时间戳。
- `MediaResource`：任务归属、原始文件名、MIME、大小、尺寸、本地存储键、公共 URL、SHA-256、状态。
- `PlatformPublishRecord`：任务、平台、适配标题/正文、标签 JSON、媒体 JSON、可空发布模式、状态、发布 URL、错误、时间戳。
- `PlatformConfig`：平台、展示名、完整 JSON 规则、启用状态。
- `PluginSession`：可选真实发布插件会话，按 `userToken + sessionId` 识别。
- 标准内容记录：`MediaRef`、`ContentBlock`、`NormalizedContent`、`AdaptedContent`。

## 7. API 与枚举约定

- API 基础前缀：`/api`。
- 媒体和页面：`/media/{mediaId}`、`/mock/{platform}/{recordId}`。
- 平台标识小写：`xiaohongshu`、`zhihu`、`wechat`、`bilibili`。
- 发布模式小写：`mock`、`real`。
- 状态值大写：`PENDING`、`ADAPTING`、`READY`、`PUBLISHING`、`SUCCESS`、`VERIFIED_SUCCESS`、`WARN`、`SUSPENDED`、`FAILED`、`SKIPPED`、`ONLINE`、`OFFLINE`。
- Java enum 可内部大写，但 Controller/DTO 边界必须转换。
- `platform_publish_record.publish_mode` 在适配占位阶段允许为空；发布阶段再写入 `mock` 或 `real`。
- `/api` 接口使用统一 JSON envelope：`code`、`message`、`data`。可预期业务错误仍返回 HTTP 200，用 JSON `code` 区分。
- WebSocket 推送必须按 `userToken` 定向发送，前端仍需按 `taskId` 二次过滤。

## 8. 前端契约

- 参考根目录 `index.html` 的 Tailwind v4、Outfit、Inter、`.glass-panel`、`.glass-header` 风格。
- 新增或修改前端时禁止固定像素宽度和硬编码比例宽度，使用响应式 Flex/Grid。
- 异步适配时 Tab 徽标、按钮和预览区应有 loading/skeleton/empty state。
- WebSocket 断线重连或初始化时，前端必须通过 HTTP 查询任务和记录状态兜底恢复。
- 预览 Tab 的人工编辑必须通过 `PUT /api/records/{recordId}` 回写后再发布。
- `real` 模式下插件离线时动态禁用发布按钮并提示原因。

## 9. Hook 与 Agent.md 更新节奏

每个模块任务开始前运行该模块 Hook。Hook 会读取根 `agent.md` 与模块 `agent.md`，并记录 3 轮 cycle。

当 Hook 提示达到 3 轮限制时，必须先更新：

- 根目录 `agent.md`
- 当前模块 `agentsPrompt/<ModuleAgent>/agent.md`

更新内容包括新增源码路径、测试路径、接口边界、跨模块依赖、配置变量和未解决风险。

## 10. 模块默认读取范围

普通模块 agent 默认读取：

- 根目录 `agent.md`
- 当前模块 `agentsPrompt/<ModuleAgent>/agent.md`
- 实际源码和测试文件

普通模块 agent 默认不读取需求大文档、接口大文档、历史 prompt、Hook 源码或规划日志。需要额外上下文时，由 Orchestrator 提取并同步到模块 `agent.md`。

## 11. 2026-05-30 Review Fix Sync

- TaskAgent now owns flexmark-based Markdown normalization, including headings, paragraphs, `-` and `*` unordered lists, ordered lists, and Markdown image nodes.
- AdaptAgent must route async completion and degradation events through OverviewAgent using `userToken`, and must restore the parent task out of `ADAPTING` after all platform futures complete.
- PublishAgent must validate adapted media references against stored `MediaResource` rows and physical files before mock publishing.
- OverviewAgent must reject WebSocket handshakes without `userToken`, keep event replay bounded, and clean stale empty session keys.
- PluginAgent heartbeat can revive the same recently expired session inside the configured grace window; older sessions must register again.
- Global LLM validation uses DeepSeek through LangChain4j OpenAI-compatible `ChatModel`; local `.env` stores the real key, `.env.example` keeps placeholders only.
- Shared JSON parsing now goes through `JsonContentMapper` helpers to reduce repeated manual `ObjectMapper` boilerplate in service code.

## 12. 2026-05-30 Round 2 Fix Sync

- TaskAgent must preserve Markdown list metadata with `ContentBlock.ordered` and `ContentBlock.depth`, and split inline images out of paragraph text.
- TaskAgent now exposes task deletion as part of CRUD; deleting a task must also remove publish records, media rows, and the local `MEDIA_STORAGE_ROOT/{taskId}` directory.
- AdaptAgent must run LLM/template adaptation on a dedicated `adaptTaskExecutor` instead of `ForkJoinPool.commonPool()`.
- PublishAgent mock HTML must use the same platform wrapper classes declared by `index.html`: `xiaohongshu-mock`, `zhihu-mock`, `wechat-mock`, and `bilibili-mock`.
