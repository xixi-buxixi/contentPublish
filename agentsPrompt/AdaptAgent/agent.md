# AdaptAgent Module Guide

## 模块功能

AdaptAgent 负责异步多平台内容适配。它从标准内容模型读取内容，结合平台配置，通过 LangChain4j 或模板降级生成平台发布记录。

## 导航

- 根总览：`../../agent.md`
- 模块目录：`agentsPrompt/AdaptAgent/`
- 未来源码建议：
  - `src/main/java/.../controller/AdaptController.java`
  - `src/main/java/.../service/AdaptService.java`
  - `src/main/java/.../adapter/PlatformAdapter.java`
  - `src/main/java/.../adapter/GenericLlmPlatformAdapter.java`
  - `src/main/java/.../adapter/TemplatePlatformAdapter.java`
  - `src/main/java/.../domain/PlatformPublishRecord.java`

## 关联模块

- TaskAgent：提供标准内容。
- MediaAgent：提供可访问媒体引用。
- ConfigAgent：提供平台规则和风格提示。
- OverviewAgent：负责把适配完成/降级事件推送给工作台。
- PublishAgent：使用适配完成的发布记录继续发布。

## 编写规范

- `POST /api/tasks/{taskId}/adapt` 必须同步创建占位记录并返回 `recordId` 列表。
- Controller 不同步等待 LLM；适配在后台线程池执行。
- 适配占位记录状态为 `ADAPTING`，完成后为 `READY`，失败为 `FAILED`。
- 缺少 API Key 或 LLM 超时时降级模板适配，并产生降级事件。
- `publish_mode` 在适配阶段允许为空。
- 不写内容任务创建、媒体物理上传、发布执行器、Mock 页面或插件接口。
- 代码出错时直接修复根因并验证。

## 接口边界

- `POST /api/tasks/{taskId}/adapt`
- `GET /api/tasks/{taskId}/records`
- `GET /api/records/{recordId}`
- `PUT /api/records/{recordId}`
- `POST /api/records/{recordId}/skip`

## Agent.md 更新

每 3 轮后更新本文件和根目录 `agent.md`，补充新增平台适配策略、降级策略、事件字段和记录字段变化。
