# PublishAgent Module Guide

## 模块功能

PublishAgent 负责一键发布、MockPublisher、发布状态机和平台拟态页面。MVP 只要求稳定完成 `mock` 模拟发布。

## 导航

- 根总览：`../../agent.md`
- 模块目录：`agentsPrompt/PublishAgent/`
- 未来源码建议：
  - `src/main/java/.../controller/PublishController.java`
  - `src/main/java/.../controller/MockPageController.java`
  - `src/main/java/.../service/PublishService.java`
  - `src/main/java/.../publisher/Publisher.java`
  - `src/main/java/.../publisher/MockPublisher.java`
  - `src/main/resources/templates/mock/*.html` 或前端路由组件

## 关联模块

- AdaptAgent：发布基于平台发布记录。
- ConfigAgent：发布前规则校验。
- MediaAgent：校验媒体 URL 可访问。
- OverviewAgent：推送发布状态事件。
- PluginAgent：仅在 `real` 模式扩展中参与。

## 编写规范

- API 入参 `mode` 使用小写 `mock` 或 `real`。
- MVP 优先实现 `mock`，`real` 只保留在线态校验和扩展点。
- 发布前校验标题、正文、标签、图片数量、媒体 URL 和记录状态。
- Mock 成功后写入 `publish_mode=mock`、`status=SUCCESS`、`published_at`、`publish_url`。
- Mock 页面要体现平台差异，不要退化成通用文章页。
- 不写内容解析、媒体物理保存、LLM 适配或插件心跳。
- 代码出错时直接修复根因并验证。

## 接口边界

- `POST /api/tasks/{taskId}/publish`
- `GET /mock/{platform}/{recordId}`
- `GET /api/mock/{platform}/{recordId}`

## Agent.md 更新

每 3 轮后更新本文件和根目录 `agent.md`，补充新增发布状态、Mock 页面路径、渲染策略和跨模块依赖。
