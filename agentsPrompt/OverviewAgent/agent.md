# OverviewAgent Module Guide

## 模块功能

OverviewAgent 是 Java 业务模块，负责全局会话、WebSocket 连接和跨模块事件路由。它让工作台、业务模块和可选浏览器插件共享同一套 `userToken` 隔离机制。

不要把本模块与当前 Codex 的 Orchestrator 协调角色混淆。Orchestrator 维护文档和调度模块；OverviewAgent 只实现会话与事件通道。

## 启动 Hook

```bash
rtk python hooks/overview_hook.py
```

若 Hook 提示 3 轮限制，先刷新根 `agent.md` 和本文件，再重新运行 Hook。

## 源码与测试导航

- `src/main/java/com/example/pulsedistro/controller/SessionController.java`
- `src/main/java/com/example/pulsedistro/config/WebSocketConfig.java`
- `src/main/java/com/example/pulsedistro/websocket/PipelineWebSocketHandler.java`
- `src/main/java/com/example/pulsedistro/event/PipelineEvent.java`
- `src/main/java/com/example/pulsedistro/service/SessionService.java`
- `src/main/java/com/example/pulsedistro/service/PipelineEventPublisher.java`
- `src/main/java/com/example/pulsedistro/dto/session/*`
- `src/test/java/com/example/pulsedistro/overview/*`

## API 与通道边界

- `POST /api/session/init`
- `GET /ws/pipeline?userToken={userToken}&traceId={traceId}`

`/api/session/init` 返回 `userToken`、`wsEndpoint` 和当前 `userToken` 的最近事件。WebSocket 是增量通道，HTTP 查询接口仍是断线恢复兜底。

## 事件模型

WebSocket 事件统一形态：

```json
{
  "event": "EVENT_NAME",
  "timestamp": 1779954061000,
  "data": {
    "userToken": "ut_xxx"
  }
}
```

核心事件：

- `PLATFORM_ADAPT_COMPLETED`
- `PLATFORM_ADAPT_DEGRADED`
- `PUBLISH_STATUS_CHANGED`
- `TASK_FAILED`
- `PLUGIN_STATUS_CHANGED`

## 关联模块

- TaskAgent：任务详情和记录查询是断线恢复基础。
- AdaptAgent：产生适配完成/降级事件。
- PublishAgent：产生发布状态事件。
- PluginAgent：产生插件在线态和真实发布回调事件。

## 编写规范

- 只处理会话初始化、WebSocket 注册、事件包装和定向推送。
- 按 `userToken` 定向发送，禁止默认广播给所有连接。
- 前端收到事件后仍需用 `taskId` 二次过滤。
- 并发写同一 WebSocket Session 时使用线程安全发送策略。
- 不写内容解析、媒体存储、LLM 适配、Mock 页面和插件填表逻辑。

## 配置变量

- `WS_ENDPOINT`
- `SERVER_PORT`

## 2026-05-30 Review Fix Sync

- WebSocket handshakes missing `userToken` are closed with `CloseStatus.BAD_DATA`.
- Closed sessions are removed from `sessionsByUser`; empty `userToken` keys are deleted.
- `PipelineEventPublisher` keeps recent event replay bounded and scheduled cleanup removes stale/empty event queues.
- `/api/session/init` accepts an empty POST body via `@RequestBody(required = false)`.
- Adapt and publish events must include `userToken`, `taskId`, `traceId`, platform/record status data when applicable.
