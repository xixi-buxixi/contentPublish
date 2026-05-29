# PluginAgent Module Guide

## 模块功能

PluginAgent 负责浏览器插件的服务端协作：注册、心跳、在线态查询和真实发布回调。它是 `real` 发布模式的可选扩展，不阻塞 MVP。

## 导航

- 根总览：`../../agent.md`
- 模块目录：`agentsPrompt/PluginAgent/`
- 未来源码建议：
  - `src/main/java/.../controller/PluginController.java`
  - `src/main/java/.../service/PluginManager.java`
  - `src/main/java/.../domain/PluginSession.java`

## 关联模块

- OverviewAgent：共享 `userToken` 会话和 WebSocket 推送。
- PublishAgent：真实发布前查询插件在线态。
- MediaAgent：插件通过 `publicUrl` 下载图片。

## 编写规范

- 插件请求必须携带 `X-User-Token`。
- 后端用 `userToken + sessionId` 识别插件会话。
- 同一 `userToken` 注册新 `sessionId` 时，旧会话标记为 `OFFLINE`。
- 心跳间隔建议 15 到 30 秒；超过 60 秒视为离线。
- 高频心跳避免 H2 长锁，可使用内存在线表加关键节点持久化。
- 插件上报 `SUSPENDED` 时更新发布记录并通知 OverviewAgent 推送状态。
- 后端不保存第三方平台账号、密码、Cookie 或 Token。
- 不写内容解析、媒体上传、LLM 适配或 Mock 页面。

## 接口边界

- `POST /api/plugin/register`
- `POST /api/plugin/heartbeat`
- `GET /api/plugin/status`
- `POST /api/plugin/publish-status`

## Agent.md 更新

每 3 轮后更新本文件和根目录 `agent.md`，补充新增插件状态、真实发布回调字段和安全约束。
