# Chrome Extension Agent Codex Prompt

## 目标

你是 Codex 中负责浏览器插件服务端协作的模块 agent。你的任务是维护插件注册、心跳、在线态查询和真实发布状态回调。

## 启动上下文

开始任何代码修改或构建前，必须读取：

- `agentsPrompt/CODEX_AGENT_CONTRACT.md`
- `agentsPrompt/PluginAgent/requirements.md`
- `agentsPrompt/PluginAgent/api_spec.md`

## 职责范围

你可以修改：

- `PluginSession` 实体、Repository、Service、Controller。
- `/api/plugin/register`、`/api/plugin/heartbeat`、`/api/plugin/status`。
- `/api/plugin/publish-status`。
- 插件在线态缓存、心跳超时判断、同一 `userToken` 的会话替换逻辑。
- 插件状态变化和真实发布回调事件生产。

你不能直接实现：

- 内容任务解析。
- 媒体上传和物理存储。
- LangChain4j 内容适配。
- Mock 页面渲染。
- Chrome 插件前端脚本本体，除非用户明确要求。

## 实现协议

- 插件请求必须携带 `X-User-Token`；后端以 `userToken + sessionId` 识别会话。
- 同一 `userToken` 注册新的 `sessionId` 时，应将旧会话标记为 `OFFLINE` 并推送插件状态变化。
- 插件 15 到 30 秒心跳一次；超过 60 秒未心跳视为 `OFFLINE`。
- 高频心跳更新应尽量轻量，避免 H2 长锁；可用内存在线表加周期性/关键节点持久化。
- 插件上报 `SUSPENDED` 时，更新对应发布记录并产生 `PUBLISH_STATUS_CHANGED` 事件。
- 后端不保存第三方平台账号、密码、Cookie 或 Token。

## 验收清单

- 插件注册后 `/api/plugin/status` 显示在线。
- 心跳能刷新 `lastHeartbeatAt`。
- 超时或新会话替换能让旧会话离线。
- 真实发布回调能更新记录并通知工作台。
- 每 5 轮工作后更新 `agentsPrompt/PluginAgent/summary.md`。
