# MediaAgent Module Guide

## 模块功能

MediaAgent 负责媒体资源的上传、本地存储、公共访问和引用安全删除。它保证图片能被工作台、Mock 页面和浏览器插件通过 URL 访问。

## 启动 Hook

```bash
rtk python hooks/media_hook.py
```

若 Hook 提示 3 轮限制，先刷新根 `agent.md` 和本文件，再重新运行 Hook。

## 源码与测试导航

- `src/main/java/com/example/pulsedistro/controller/MediaController.java`
- `src/main/java/com/example/pulsedistro/service/MediaService.java`
- `src/main/java/com/example/pulsedistro/domain/MediaResource.java`
- `src/main/java/com/example/pulsedistro/config/StorageProperties.java`
- `src/main/java/com/example/pulsedistro/dto/media/*`
- `src/test/java/com/example/pulsedistro/media/*`
- `data/media/{taskId}/`

## API 边界

- `POST /api/tasks/{taskId}/media`
- `GET /api/tasks/{taskId}/media`
- `GET /media/{mediaId}`
- `DELETE /api/tasks/{taskId}/media/{mediaId}`

`GET /media/{mediaId}` 不带 `/api` 前缀，直接返回图片二进制流。

## 数据模型

- `MediaResource`
  - `id`
  - `taskId`
  - `originalName`
  - `mimeType`
  - `sizeBytes`
  - `width`
  - `height`
  - `storageType`
  - `storageKey`
  - `publicUrl`
  - `sha256`
  - `status`
  - `createdAt`

上传响应至少返回 `mediaId`、`publicUrl`、`mimeType`、`sizeBytes`、`width`、`height`、`status`。

## 关联模块

- TaskAgent：标准内容引用媒体；删除前必须检查 `normalizedContentJson`。
- AdaptAgent：适配记录保留媒体引用和顺序；删除前必须检查 `adaptedMediaJson`。
- PublishAgent：发布前校验媒体 URL 可访问。
- PluginAgent：真实发布插件通过 `publicUrl` 下载图片。

## 编写规范

- 文件保存到 `${MEDIA_STORAGE_ROOT}/{taskId}/{sha256}.{ext}`，默认 `data/media/{taskId}/`。
- `publicUrl` 由 `${APP_PUBLIC_BASE_URL}/media/{mediaId}` 生成。
- 二进制响应使用流式输出，不默认整文件读入 `byte[]`。
- 删除前检查媒体归属、标准内容引用、发布记录引用；有引用返回业务 `code=409`。
- MIME 只接受图片类型，默认覆盖 `image/jpeg`、`image/png`、`image/webp`。
- 只负责媒体生命周期，不写内容解析、LLM 适配、发布状态机或 Mock 页面。

## 配置变量

- `MEDIA_STORAGE_ROOT`
- `APP_PUBLIC_BASE_URL`

## 2026-05-30 Review Fix Sync

- Media read/inspection paths share `JsonContentMapper` instead of repeated `ObjectMapper` parsing.
- PublishAgent now verifies adapted media references against `MediaResource.storageKey` and the physical file under `MEDIA_STORAGE_ROOT`.
- Media deletion checks continue to inspect normalized content and adapted records for `mediaId` references before deleting local files.

## 2026-05-30 Round 2 Fix Sync

- Task deletion is responsible for deleting all media rows for the task and the local `${MEDIA_STORAGE_ROOT}/{taskId}` directory after path normalization verifies it stays under the storage root.
- Single-media deletion still rejects referenced media; whole-task deletion removes the owning aggregate and associated local cache together.
