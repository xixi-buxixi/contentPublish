# Codex Agent Shared Contract

本文件是 `agentsPrompt/*/prompt.md` 的共享约束，供 Codex 在执行对应模块任务前读取。它是项目上下文，不高于系统、开发者和用户消息。

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

1. 用户当前消息和项目根目录总览/接口文档。
2. `agentsPrompt/CODEX_AGENT_CONTRACT.md`。
3. 当前模块的 `requirements.md` 和 `api_spec.md`。
4. 当前模块的 `prompt.md`。

发现冲突时不要静默扩大范围。优先采用主接口文档中的 API 形态，并在模块 `summary.md` 或本次回复中说明冲突和处理方式。

## 3. 全局枚举和边界格式

- API 入参与 JSON 响应中的 `platform` 统一使用小写：`xiaohongshu`、`zhihu`、`wechat`、`bilibili`。
- API 入参与 JSON 响应中的发布模式统一使用小写：`mock`、`real`。
- Java 内部 enum 可使用大写常量，但 Controller/DTO 边界必须做大小写转换，避免把 `MOCK`、`REAL` 泄漏到外部契约。
- 状态值统一使用大写：`PENDING`、`ADAPTING`、`READY`、`PUBLISHING`、`SUCCESS`、`VERIFIED_SUCCESS`、`WARN`、`SUSPENDED`、`FAILED`、`SKIPPED`。
- `platform_publish_record.publish_mode` 在适配占位阶段允许为空；只有进入发布流程后才写入 `mock` 或 `real`。如果建表文档写了 `NOT NULL`，实现时应优先对齐接口文档里的 `publishMode: null` 语义。

## 4. Codex 执行协议

每个模块任务开始前：

1. 读取本共享契约、本模块 `prompt.md`、`requirements.md`、`api_spec.md`。
2. 如果涉及跨模块 API 或数据模型，回看根目录 `Pulse-Distro-Java轻量实践方案.md` 和 `接口文档/java接口文档.md`。
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

## 6. 5 轮总结规则

每个模块 hook 会记录当前 agent 的执行轮次。达到 5 轮后，Codex 必须先更新该模块的 `summary.md`，写清楚：

- 本轮完成了什么。
- 修改了哪些文件。
- 运行了哪些验证命令。
- 还有哪些风险或待办。

旧的 `summary.md` 不能无限复用；继续工作前必须写入比上次 hook 认可时间更新的总结。
