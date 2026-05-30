# PluginAgent Module Guide

## 模块功能

PluginAgent 负责浏览器插件的服务端协作：注册、心跳、在线态查询和真实发布回调。它是 `real` 发布模式的可选扩展，不阻塞 MVP。

## 启动 Hook

```bash
rtk python hooks/plugin_hook.py
```

若 Hook 提示 3 轮限制，先刷新根 `agent.md` 和本文件，再重新运行 Hook。

## 源码与测试导航

- `src/main/java/com/example/pulsedistro/controller/PluginController.java`
- `src/main/java/com/example/pulsedistro/service/PluginManager.java`
- `src/main/java/com/example/pulsedistro/domain/PluginSession.java`
- `src/main/java/com/example/pulsedistro/dto/plugin/*`
- `src/test/java/com/example/pulsedistro/plugin/*`

## API 边界

- `POST /api/plugin/register`
- `POST /api/plugin/heartbeat`
- `GET /api/plugin/status`
- `POST /api/plugin/publish-status`

插件请求必须携带 `X-User-Token`。真实发布前，PublishAgent 通过本模块确认同一 `userToken` 下插件在线。

## 数据模型

- `PluginSession`
  - `id`
  - `userToken`
  - `sessionId`
  - `extensionVersion`
  - `browser`
  - `status`
  - `lastHeartbeatAt`
  - `createdAt`

状态建议使用 `ONLINE`、`OFFLINE`、`SUSPENDED`。

## 关联模块

- OverviewAgent：共享 `userToken` 会话并推送 `PLUGIN_STATUS_CHANGED`。
- PublishAgent：`real` 发布前查询插件在线态；插件上报状态后更新发布记录。
- MediaAgent：插件通过 `publicUrl` 下载图片。

## 编写规范

- 后端用 `userToken + sessionId` 识别插件会话。
- 同一 `userToken` 注册新 `sessionId` 时，旧会话标记为 `OFFLINE`。
- 心跳间隔建议 15 到 30 秒；超过 60 秒视为离线。
- 高频心跳避免 H2 长锁，可使用内存在线表加关键节点持久化。
- 插件上报 `SUSPENDED` 时更新发布记录并通知 OverviewAgent 推送状态。
- 后端不保存第三方平台账号、密码、Cookie 或 Token。
- 不写内容解析、媒体上传、LLM 适配或 Mock 页面。

## 配置变量

- `WS_ENDPOINT`
- 可选后续变量：`PLUGIN_HEARTBEAT_TTL_SECONDS`

## 2026-05-30 Review Fix Sync

- Heartbeat can revive the same `userToken + sessionId` if it expired less than 5 minutes ago.
- Different session IDs, missing sessions, or sessions older than the recovery grace must register again.
- `PluginManager` uses an injectable `Clock` for deterministic tests and scheduled cleanup removes stale in-memory plugin sessions.
- `PLUGIN_STATUS_CHANGED` is emitted when a recently expired session revives through heartbeat.
