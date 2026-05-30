# PublishAgent Module Guide

## 模块功能

PublishAgent 负责一键发布、MockPublisher、发布状态机和平台拟态页面。MVP 只要求稳定完成 `mock` 模拟发布，`real` 仅保留插件在线态校验和扩展点。

## 启动 Hook

```bash
rtk python hooks/publish_hook.py
```

若 Hook 提示 3 轮限制，先刷新根 `agent.md` 和本文件，再重新运行 Hook。

## 源码与测试导航

- `src/main/java/com/example/pulsedistro/controller/PublishController.java`
- `src/main/java/com/example/pulsedistro/controller/MockPageController.java`
- `src/main/java/com/example/pulsedistro/service/PublishService.java`
- `src/main/java/com/example/pulsedistro/publisher/Publisher.java`
- `src/main/java/com/example/pulsedistro/publisher/MockPublisher.java`
- `src/main/java/com/example/pulsedistro/dto/publish/*`
- `src/main/resources/static/mock/*` 或 `src/main/resources/templates/mock/*`
- `src/test/java/com/example/pulsedistro/publish/*`

## API 边界

- `POST /api/tasks/{taskId}/publish`
- `GET /mock/{platform}/{recordId}`
- `GET /api/mock/{platform}/{recordId}`

若前端使用 hash 路由渲染 Mock 页面，后端保留 `/api/mock/{platform}/{recordId}` JSON 数据接口即可。

## 数据模型

- 发布基于 `PlatformPublishRecord`。
- Mock 成功后写入：
  - `publishMode=mock`
  - `status=SUCCESS`
  - `publishUrl`
  - `publishedAt`
- 发布响应返回 `taskId`、`mode`、`results[]`，每项包含 `recordId`、`platform`、`status`、`publishUrl`。

## 关联模块

- AdaptAgent：发布基于 `READY` 平台发布记录。
- ConfigAgent：发布前规则校验。
- MediaAgent：校验媒体 URL 可访问。
- OverviewAgent：推送 `PUBLISH_STATUS_CHANGED` 和失败事件。
- PluginAgent：仅 `real` 模式查询插件在线态。

## 编写规范

- API 入参 `mode` 使用小写 `mock` 或 `real`。
- 发布前校验标题、正文、标签、图片数量、媒体 URL 和记录状态。
- `mock` 模式必须生成可访问拟态页面或可由前端渲染的 Mock 数据。
- `real` 模式如果插件离线，返回业务 `code=400`，`reason=PLUGIN_OFFLINE`，不要阻塞 MVP。
- Mock 页面要体现平台差异：小红书、知乎、微信公众号、B 站不能退化为同一通用文章页。
- 只写发布与 Mock，不写内容解析、媒体物理保存、LLM 适配或插件心跳。

## 配置变量

- `APP_PUBLIC_BASE_URL`
- `WS_ENDPOINT`

## 2026-05-30 Review Fix Sync

- Mock publish validates every adapted media item before writing `SUCCESS`: media must have an uploaded `mediaId`, belong to the task, resolve under `MEDIA_STORAGE_ROOT`, and exist as a local file.
- External Markdown image URLs may appear in normalized/adapted preview data, but publish rejects them until uploaded/normalized into `MediaResource`.
- `/mock/{platform}/{recordId}` renders platform-specific HTML instead of one generic card:
  - `xiaohongshu`: mobile shell/feed style.
  - `zhihu`: Q&A/article answer style.
  - `wechat`: public-account article style.
  - `bilibili`: dynamic/column card style.
- Mock page CSS keeps responsive `min(100%, ...)`, glass styling, Outfit/Inter fonts, and escapes Java formatter percent signs.
