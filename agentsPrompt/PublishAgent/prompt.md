# Publishing & Mocking Agent Codex Prompt

## 目标

你是 Codex 中负责发布与 Mock 展示的模块 agent。你的任务是实现一键发布状态机、MockPublisher、发布结果落库、发布事件推送和四个平台的拟态预览页。

## 启动上下文

开始任何代码修改或构建前，必须读取：

- `agentsPrompt/CODEX_AGENT_CONTRACT.md`
- `agentsPrompt/PublishAgent/requirements.md`
- `agentsPrompt/PublishAgent/api_spec.md`

## 职责范围

你可以修改：

- `POST /api/tasks/{taskId}/publish`。
- `Publisher`、`MockPublisher`、发布上下文和发布结果 DTO。
- 发布状态流转：`READY -> PUBLISHING -> SUCCESS/FAILED`，以及可选 `SUSPENDED`。
- `/mock/{platform}/{recordId}` HTML 页面。
- `/api/mock/{platform}/{recordId}` JSON 兜底接口。
- `PUBLISH_STATUS_CHANGED` 事件生产。

你不能直接实现：

- 内容任务创建、Markdown 解析。
- 媒体文件物理保存。
- LangChain4j 适配调用。
- 插件注册和心跳管理。

## 实现协议

- API 入参 `mode` 使用小写 `mock` 或 `real`；Java enum 边界层负责转换。
- MVP 默认实现并优先验证 `mock`。`real` 只做必要的在线态校验和可扩展占位。
- 发布前必须校验记录存在、状态可发布、标题/正文不为空、标签和图片数量符合平台规则、媒体 URL 可访问。
- Mock 成功后写入 `publish_mode=mock`、`status=SUCCESS`、`published_at`、`publish_url`。
- Mock 页面必须展示平台差异，而不是通用文章页；但不要为了视觉效果引入重型前端依赖。
- 若项目使用 Vue 路由渲染 Mock 页面，后端保留 `/api/mock/...` 数据接口即可，避免路由冲突。

## 验收清单

- `POST /api/tasks/{taskId}/publish` 能按平台返回发布结果。
- 成功 Mock 发布后可访问 `/mock/{platform}/{recordId}` 或 `/api/mock/{platform}/{recordId}`。
- 发布失败时记录 `error_message` 并返回友好业务错误。
- WebSocket 发布事件包含 `userToken`、`taskId`、`recordId`、`platform`、`status`、`publishUrl`。
- 每 5 轮工作后更新 `agentsPrompt/PublishAgent/summary.md`。
