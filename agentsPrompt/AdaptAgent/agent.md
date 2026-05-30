# AdaptAgent Module Guide

## 模块功能

AdaptAgent 负责异步多平台内容适配。它从标准内容模型读取内容，结合平台配置，通过 LangChain4j 或模板降级生成平台发布记录。

## 启动 Hook

```bash
rtk python hooks/adapt_hook.py
```

若 Hook 提示 3 轮限制，先刷新根 `agent.md` 和本文件，再重新运行 Hook。

## 源码与测试导航

- `src/main/java/com/example/pulsedistro/controller/AdaptController.java`
- `src/main/java/com/example/pulsedistro/controller/RecordController.java`
- `src/main/java/com/example/pulsedistro/service/AdaptService.java`
- `src/main/java/com/example/pulsedistro/adapter/PlatformAdapter.java`
- `src/main/java/com/example/pulsedistro/adapter/GenericLlmPlatformAdapter.java`
- `src/main/java/com/example/pulsedistro/adapter/TemplatePlatformAdapter.java`
- `src/main/java/com/example/pulsedistro/domain/PlatformPublishRecord.java`
- `src/main/java/com/example/pulsedistro/dto/adapt/*`
- `src/test/java/com/example/pulsedistro/adapt/*`

## API 边界

- `POST /api/tasks/{taskId}/adapt`
- `GET /api/tasks/{taskId}/records`
- `GET /api/records/{recordId}`
- `PUT /api/records/{recordId}`
- `POST /api/records/{recordId}/skip`

`POST /api/tasks/{taskId}/adapt` 必须同步创建每个平台的 `ADAPTING` 占位记录并返回 `recordId` 列表，然后后台异步执行适配。

## 数据模型

- `PlatformPublishRecord`
  - `id`
  - `taskId`
  - `platform`
  - `adaptedTitle`
  - `adaptedContent`
  - `tagsJson`
  - `adaptedMediaJson`
  - `publishMode`：适配阶段允许为空
  - `status`
  - `publishUrl`
  - `errorMessage`
  - `createdAt`
  - `publishedAt`
- `AdaptedContent`
  - `platform`
  - `title`
  - `content`
  - `tags`
  - `media`
  - `styleExplanation`

## 关联模块

- TaskAgent：提供 `NormalizedContent`。
- MediaAgent：提供可访问媒体引用。
- ConfigAgent：提供平台规则和风格提示。
- OverviewAgent：推送 `PLATFORM_ADAPT_COMPLETED` 与 `PLATFORM_ADAPT_DEGRADED`。
- PublishAgent：使用 `READY` 发布记录继续发布。

## 编写规范

- Controller 不同步等待 LLM。
- 后台线程池执行平台适配，单个平台失败不能阻塞其他平台。
- 完成状态为 `READY`，失败为 `FAILED`，跳过为 `SKIPPED`。
- 缺少 `LANGCHAIN4J_API_KEY` 或 LLM 超时时降级模板适配，并产生降级事件。
- `PUT /api/records/{recordId}` 保存人工编辑后的标题、正文、标签和媒体顺序，发布以该记录为准。
- API 入参/出参平台小写，状态大写。
- 只写适配和记录管理，不写内容任务创建、媒体物理上传、发布执行器、Mock 页面或插件接口。

## 配置变量

- `LANGCHAIN4J_API_KEY`
- `LANGCHAIN4J_BASE_URL`
- 可选后续变量：`LANGCHAIN4J_MODEL_NAME`、`ADAPT_TIMEOUT_SECONDS`

## 2026-05-30 Review Fix Sync

- `POST /api/tasks/{taskId}/adapt` reads `X-User-Token` and `X-Trace-Id`; both are passed into `AdaptService`.
- `AdaptationModelClient` calls a LangChain4j OpenAI-compatible `ChatModel` when `LANGCHAIN4J_API_KEY` is configured. Missing key, timeout, API error, or JSON parse failure falls back to template generation.
- DeepSeek validation defaults: `LANGCHAIN4J_BASE_URL=https://api.deepseek.com`, `LANGCHAIN4J_MODEL_NAME=deepseek-v4-flash`.
- Each platform completion publishes `PLATFORM_ADAPT_COMPLETED`; template fallback publishes `PLATFORM_ADAPT_DEGRADED`; failed platform records publish `TASK_FAILED`.
- `CompletableFuture.allOf(...)` updates the parent task from `ADAPTING` to `READY` after platform futures finish, or `FAILED` if all platform records failed.

## 2026-05-30 Round 2 Fix Sync

- Adaptation futures must use an injected `adaptTaskExecutor` with thread names beginning `adapt-`; do not call `CompletableFuture.runAsync(...)` without an executor.
- Tests clear `LANGCHAIN4J_API_KEY` unless they intentionally validate the real DeepSeek path.
