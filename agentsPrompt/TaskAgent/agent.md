# TaskAgent Module Guide

## 模块功能

TaskAgent 负责内容任务、原始内容解析和标准内容模型。它把用户输入的文本或 Markdown 转换为后续适配模块可消费的结构化内容。

## 启动 Hook

```bash
rtk python hooks/task_hook.py
```

若 Hook 提示 3 轮限制，先刷新根 `agent.md` 和本文件，再重新运行 Hook。

## 源码与测试导航

- `src/main/java/com/example/pulsedistro/controller/TaskController.java`
- `src/main/java/com/example/pulsedistro/service/TaskService.java`
- `src/main/java/com/example/pulsedistro/domain/ContentTask.java`
- `src/main/java/com/example/pulsedistro/model/NormalizedContent.java`
- `src/main/java/com/example/pulsedistro/model/ContentBlock.java`
- `src/main/java/com/example/pulsedistro/model/MediaRef.java`
- `src/main/java/com/example/pulsedistro/dto/task/*`
- `src/test/java/com/example/pulsedistro/task/*`

## API 边界

- `POST /api/tasks`
- `GET /api/tasks/{taskId}`
- `GET /api/tasks/{taskId}/normalized`
- `PUT /api/tasks/{taskId}/normalized`
- `DELETE /api/tasks/{taskId}`

统一响应使用 `code`、`message`、`data`。任务状态使用大写，例如 `PENDING`、`ADAPTING`、`READY`。

## 数据模型

- `ContentTask`
  - `id`
  - `title`
  - `sourceType`
  - `rawContent`
  - `normalizedContentJson`
  - `coverMediaId`
  - `status`
  - `createdAt`
  - `updatedAt`
- `NormalizedContent`
  - `title`
  - `summary`
  - `blocks`
- `ContentBlock`
  - `type`: `heading`、`paragraph`、`list`、`image`
  - `level`
  - `text`
  - `media`
  - `ordered`: list block only, `true` for ordered list and `false` for unordered list
  - `depth`: list block only, 0-based nesting depth
- `MediaRef`
  - `mediaId`
  - `publicUrl`
  - `alt`
  - `width`
  - `height`

## 关联模块

- MediaAgent：标准内容中的图片只信任已入库且属于当前任务的 `mediaId`。
- AdaptAgent：读取标准内容并生成平台适配记录。
- PublishAgent：发布使用平台记录，不直接修改标准内容。
- OverviewAgent：任务状态变化可作为断线恢复和事件展示的基础。

## 编写规范

- 标准内容 JSON 使用 Jackson/JPA Converter 序列化，禁止手写 JSON 拼接。
- 更新标准内容时只信任 `mediaId`，必须重新查询媒体记录补齐 `publicUrl`、尺寸和状态。
- 禁止保存 `local://`、浏览器临时 URL 或未入库图片路径。
- 修改标准内容后，要明确处理已有适配结果是否失效；MVP 可将相关发布记录标记为 `PENDING` 或要求重新适配。
- 只做任务与标准内容，不写 Multipart 上传、二进制媒体输出、LLM 调用、发布器、Mock 页面或插件接口。
- 行为变更优先先写失败测试，再实现。

## 配置变量

- 读取通用 `SERVER_PORT`、`APP_PUBLIC_BASE_URL` 即可。
- 不直接读取 `MEDIA_STORAGE_ROOT` 或 `LANGCHAIN4J_API_KEY`。

## 2026-05-30 Review Fix Sync

- Markdown parsing must use flexmark rather than line-only regex parsing.
- Supported blocks include `heading`, `paragraph`, `list`, and `image`.
- List parsing must preserve both unordered (`-` and `*`) and ordered Markdown list items in normalized list blocks.
- Markdown image nodes must become `image` blocks. If the URL is not an uploaded media ID, preserve it as external URL metadata for preview, but publish must later require upload/normalization.
- Add focused tests before parser behavior changes.

## 2026-05-30 Round 2 Fix Sync

- `DELETE /api/tasks/{taskId}` removes publish records, media rows, the task row, and `${MEDIA_STORAGE_ROOT}/{taskId}` after path normalization verifies the target stays under the media root.
- Markdown list extraction emits one `list` block per item and preserves `ordered` plus `depth`; nested list text must not be appended to the parent item.
- Paragraph inline images are split in document order into `paragraph` / `image` / `paragraph` blocks, so image alt text does not pollute paragraph text.

## 2026-05-30 Review Fix Plan Sync

- Markdown inline extraction must preserve common inline Markdown marks (`**bold**`, `*italic*`, inline code, strike) for downstream platforms that support Markdown.
- List item extraction must emit image blocks for inline images inside list items while keeping `ordered` and `depth` metadata on list text blocks.
- `deleteTask` must not let post-delete filesystem cleanup failures roll back committed database deletion; schedule media directory removal after transaction commit and log failures.
