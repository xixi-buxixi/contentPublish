# OverviewAgent Module Guide

## 模块功能

OverviewAgent 负责全局会话、WebSocket 连接和跨模块事件路由。它让工作台、业务模块和可选浏览器插件共享同一套 `userToken` 隔离机制。

## 导航

- 根总览：`../../agent.md`
- 模块目录：`agentsPrompt/OverviewAgent/`
- 未来源码建议：
  - `src/main/java/.../controller/SessionController.java`
  - `src/main/java/.../config/WebSocketConfig.java`
  - `src/main/java/.../websocket/PipelineWebSocketHandler.java`
  - `src/main/java/.../event/PipelineEvent.java`
  - `src/main/java/.../service/SessionService.java`

## 关联模块

- TaskAgent：任务创建和状态查询是断线恢复的基础。
- AdaptAgent：产生适配完成/降级事件。
- PublishAgent：产生发布状态事件。
- PluginAgent：产生插件在线态和真实发布回调事件。

## 编写规范

- 只处理会话初始化、WebSocket 注册、事件包装和定向推送。
- 不写内容解析、媒体存储、LLM 适配、Mock 页面和插件填表逻辑。
- WebSocket 事件统一包含 `event`、`timestamp`、`data.userToken`。
- 按 `userToken` 定向发送，禁止默认广播给所有连接。
- 并发写同一 WebSocket Session 时使用线程安全发送策略。
- 代码出错时直接定位根因并修改源码，同时补充或运行验证；不要只做表面补丁。

## 接口边界

- `POST /api/session/init` 返回 `userId`、`userToken`、`traceId`。
- `GET /ws/pipeline?userToken=...&traceId=...` 建立长连接。
- WebSocket 只是增量通道，HTTP 查询接口仍是断线恢复兜底。

## Agent.md 更新

每 3 轮后更新本文件和根目录 `agent.md`，补充新增事件类型、源码位置、测试位置和跨模块依赖。
