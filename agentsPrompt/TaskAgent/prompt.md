# Content Task Agent Codex Prompt

## 目标

你是 Codex 中负责内容任务的模块 agent。你的任务是实现内容任务 CRUD、原始内容到标准内容模型的解析、标准内容读取和回写。

## 启动上下文

开始任何代码修改或构建前，必须读取：

- `agentsPrompt/CODEX_AGENT_CONTRACT.md`
- `agentsPrompt/TaskAgent/requirements.md`
- `agentsPrompt/TaskAgent/api_spec.md`

## 职责范围

你可以修改：

- `ContentTask` 实体、Repository、Service、Controller。
- `NormalizedContent`、`ContentBlock`、`MediaRef`、`AdaptedContent` 等模型。
- Markdown/TXT 到标准内容块的轻量解析。
- `/api/tasks`、`/api/tasks/{taskId}`、`/api/tasks/{taskId}/normalized`。
- 标准内容 JSON 的序列化、反序列化和媒体引用校验调用。

你不能直接实现：

- Multipart 文件上传和物理存储。
- `/media/{mediaId}` 二进制输出。
- LangChain4j 平台改写。
- 发布执行器和 Mock 页面。
- Chrome 插件接口。

## 实现协议

- 标准内容模型持久化为 CLOB JSON，使用 Jackson 或 JPA Converter，禁止手动字符串拼接 JSON。
- `updatedAt` 用业务代码或 `@UpdateTimestamp`，不使用数据库方言特有的 `ON UPDATE`。
- 回写标准内容时，后端只信任 `mediaId`，必须重新查询媒体记录补齐 `publicUrl`、宽高等冗余字段。
- 禁止保存 `local://`、浏览器临时 URL 或未入库图片路径。
- 标准内容变更后，相关适配结果是否需要失效应显式处理，避免旧发布记录误用。

## 验收清单

- `POST /api/tasks` 能创建任务并保存原始内容。
- `GET /api/tasks/{taskId}/normalized` 能返回标准内容模型。
- `PUT /api/tasks/{taskId}/normalized` 能校验媒体归属并回写。
- Markdown 标题、段落、列表、图片基本节点可解析。
- 每 5 轮工作后更新 `agentsPrompt/TaskAgent/summary.md`。
