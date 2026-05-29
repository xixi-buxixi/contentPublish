# MediaAgent Module Guide

## 模块功能

MediaAgent 负责媒体资源的上传、本地存储、公共访问和引用安全删除。它保证图片能被工作台、Mock 页面和浏览器插件通过 URL 访问。

## 导航

- 根总览：`../../agent.md`
- 模块目录：`agentsPrompt/MediaAgent/`
- 未来源码建议：
  - `src/main/java/.../controller/MediaController.java`
  - `src/main/java/.../service/MediaService.java`
  - `src/main/java/.../domain/MediaResource.java`
  - `data/media/{taskId}/`

## 关联模块

- TaskAgent：标准内容引用媒体。
- AdaptAgent：平台适配结果保留媒体顺序和封面引用。
- PublishAgent：发布前校验媒体 URL 可访问。
- PluginAgent：真实发布时插件通过 `publicUrl` 下载图片。

## 编写规范

- 文件保存到 `data/media/{taskId}/{sha256}.{ext}`。
- `publicUrl` 默认为 `http://localhost:8080/media/{mediaId}` 或配置生成的等价地址。
- 二进制响应使用流式输出，不默认整文件读入 `byte[]`。
- 删除前检查媒体归属、标准内容引用、发布记录引用；有引用返回业务 `code=409`。
- 只负责媒体生命周期，不写内容解析、LLM 适配、发布状态机或 Mock 页面。
- 代码出错时直接修复根因并验证。

## 接口边界

- `POST /api/tasks/{taskId}/media`
- `GET /api/tasks/{taskId}/media`
- `GET /media/{mediaId}`
- `DELETE /api/tasks/{taskId}/media/{mediaId}`

## Agent.md 更新

每 3 轮后更新本文件和根目录 `agent.md`，补充新增存储配置、媒体字段、引用校验规则和测试位置。
