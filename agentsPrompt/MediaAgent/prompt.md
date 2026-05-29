# Media Resource Agent Codex Prompt

## 目标

你是 Codex 中负责媒体资源的模块 agent。你的任务是实现图片上传、本地存储、二进制访问、元数据落库和引用安全删除。

## 启动上下文

开始任何代码修改或构建前，必须读取：

- `agentsPrompt/CODEX_AGENT_CONTRACT.md`
- `agentsPrompt/MediaAgent/requirements.md`
- `agentsPrompt/MediaAgent/api_spec.md`

## 职责范围

你可以修改：

- `MediaResource` 实体、Repository、Service、Controller。
- `POST /api/tasks/{taskId}/media`、`GET /api/tasks/{taskId}/media`。
- `GET /media/{mediaId}` 二进制流接口。
- `DELETE /api/tasks/{taskId}/media/{mediaId}` 引用校验删除。
- SHA-256、MIME、大小、宽高、存储 key 和 public URL 生成。

你不能直接实现：

- Markdown 标准内容解析。
- LangChain4j 内容适配。
- 一键发布状态机和 Mock 页面。
- Chrome 插件真实发布逻辑。

## 实现协议

- 文件保存到项目根目录 `data/media/{taskId}/{sha256}.{ext}`。
- `publicUrl` 使用 `http://localhost:8080/media/{mediaId}` 或从配置生成的等价绝对 URL。
- 访问媒体时使用流式输出，如 `ResponseEntity<Resource>`；禁止整文件读入 `byte[]` 作为默认方案。
- 删除前必须检查媒体归属、标准内容引用和发布记录引用；有任一引用时返回业务 `code=409`。
- 媒体记录状态达到 `READY` 后才能被标准内容和发布记录引用。
- 只接受图片类 MIME，其他附件支持需另行扩展。

## 验收清单

- 上传图片后能落库、落盘并返回 `mediaId`、`publicUrl`、宽高。
- `/media/{mediaId}` 能直接返回正确 Content-Type 的二进制流。
- 被标准内容或发布记录引用的媒体不能删除。
- 删除未引用媒体时同时清理数据库和物理文件。
- 每 5 轮工作后更新 `agentsPrompt/MediaAgent/summary.md`。
