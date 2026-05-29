# Codex Agent Shared Contract

本文件是历史共享约束，默认不再由普通模块 agent 直接读取。新的默认入口是根目录 `agent.md` 和当前模块 `agentsPrompt/<ModuleAgent>/agent.md`。本文件仅供总览 agent 或人工维护上下文时参考。

## 1. 项目主线

Pulse Distro 的 MVP 采用 Java 21 + Spring Boot 3.3.x 单体应用：

- 前端：Vue 3 静态资源打包进 Spring Boot `static`。
- 后端：Spring Boot HTTP + WebSocket。
- 存储：H2 文件数据库 + 本地媒体目录 `data/media/{taskId}/`。
- 智能适配：LangChain4j 优先，缺少 API Key 或超时时降级为模板规则。
- 发布主线：`mock` 模拟发布是 MVP 必做能力。
- 真实发布：`real` 浏览器插件发布是可选扩展，不得阻塞 MVP。

## 2. 契约优先级

当文档出现冲突时，Codex 按以下优先级处理：

1. 用户当前消息。
2. 根目录 `agent.md`。
3. 当前模块 `agentsPrompt/<ModuleAgent>/agent.md`。
4. 总览 agent 从需求/接口资料中提取并注入的模块上下文。

发现冲突时不要静默扩大范围。优先采用 `agent.md` 中已经注入的约定，并在本次回复或相关 `agent.md` 更新中说明冲突和处理方式。

## 3. 全局枚举和边界格式

- API 入参与 JSON 响应中的 `platform` 统一使用小写：`xiaohongshu`、`zhihu`、`wechat`、`bilibili`。
- API 入参与 JSON 响应中的发布模式统一使用小写：`mock`、`real`。
- Java 内部 enum 可使用大写常量，但 Controller/DTO 边界必须做大小写转换，避免把 `MOCK`、`REAL` 泄漏到外部契约。
- 状态值统一使用大写：`PENDING`、`ADAPTING`、`READY`、`PUBLISHING`、`SUCCESS`、`VERIFIED_SUCCESS`、`WARN`、`SUSPENDED`、`FAILED`、`SKIPPED`。
- `platform_publish_record.publish_mode` 在适配占位阶段允许为空；只有进入发布流程后才写入 `mock` 或 `real`。如果建表文档写了 `NOT NULL`，实现时应优先对齐接口文档里的 `publishMode: null` 语义。

## 4. Codex 执行协议

每个模块任务开始前：

1. 读取根目录 `agent.md` 和当前模块 `agentsPrompt/<ModuleAgent>/agent.md`。
2. 不默认读取需求文档、接口文档、prompt 或 hook。需要额外上下文时，由总览 agent 提取并更新对应 `agent.md`。
3. 明确本次只修改当前模块职责内的代码；跨模块改动必须是接口 DTO、事件或契约所需的最小变更。
4. 行为代码变更优先写测试；无法自动测试时，说明原因并做可复现的手工验证。

## 5. WebSocket 和会话约束

- 工作台首次打开必须通过 `/api/session/init` 获取 `userId`、`userToken`、`traceId`。
- 普通 API 建议携带 `X-User-Token` 和 `X-Trace-Id`。
- WebSocket 地址：`/ws/pipeline?userToken={userToken}&traceId={traceId}`。
- 后端推送必须按 `userToken` 定向发送，前端收到后仍需用 `taskId` 做二次过滤。
- WebSocket 事件统一形态：

```json
{
  "event": "EVENT_NAME",
  "timestamp": 1779954061000,
  "data": {
    "userToken": "ut_xxx"
  }
}
```

## 6. 3 轮 Agent.md 更新规则

每个模块 hook 会记录当前 agent 的执行轮次。达到 3 轮后，Codex 必须先更新：

- 根目录 `agent.md`
- 当前模块 `agentsPrompt/<ModuleAgent>/agent.md`

更新内容包括模块职责、导航、关联模块、接口边界、编写规范或未解决风险。旧的 `agent.md` 不能无限复用；继续工作前必须让这些文件比上次 hook 认可时间更新。
