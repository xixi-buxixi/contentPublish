# TaskAgent Module Guide

## 模块功能

TaskAgent 负责内容任务、原始内容解析和标准内容模型。它把用户输入的文本或 Markdown 转换为后续适配模块可消费的结构化内容。

## 导航

- 根总览：`../../agent.md`
- 模块目录：`agentsPrompt/TaskAgent/`
- 未来源码建议：
  - `src/main/java/.../controller/TaskController.java`
  - `src/main/java/.../service/TaskService.java`
  - `src/main/java/.../domain/ContentTask.java`
  - `src/main/java/.../model/NormalizedContent.java`
  - `src/main/java/.../model/ContentBlock.java`
  - `src/main/java/.../model/MediaRef.java`

## 关联模块

- MediaAgent：标准内容中的图片必须引用已入库的 `mediaId`。
- AdaptAgent：读取标准内容并生成平台适配内容。
- PublishAgent：发布时使用用户确认后的平台记录，不直接改标准内容。

## 编写规范

- 标准内容 JSON 使用 Jackson/JPA Converter 序列化，禁止手动拼 JSON。
- 更新标准内容时只信任 `mediaId`，必须重新查询媒体记录补齐 `publicUrl`。
- 禁止保存 `local://`、浏览器临时 URL 或未入库图片路径。
- 修改标准内容后，要明确处理已有适配结果是否失效。
- 不写 Multipart 上传、二进制媒体输出、LLM 调用、发布器或 Mock 页面。
- 代码出错时直接修复根因并补充验证，不要绕开问题。

## 接口边界

- `POST /api/tasks`
- `GET /api/tasks/{taskId}`
- `GET /api/tasks/{taskId}/normalized`
- `PUT /api/tasks/{taskId}/normalized`

## Agent.md 更新

每 3 轮后更新本文件和根目录 `agent.md`，补充新增模型、解析规则、接口字段和依赖变化。
