# ConfigAgent Module Guide

## 模块功能

ConfigAgent 负责平台规则配置。它把各平台的字数、标签、图片、格式能力和风格提示沉淀成可查询规则，供前端、适配和发布模块复用。

## 导航

- 根总览：`../../agent.md`
- 模块目录：`agentsPrompt/ConfigAgent/`
- 未来源码建议：
  - `src/main/java/.../controller/PlatformConfigController.java`
  - `src/main/java/.../service/PlatformConfigService.java`
  - `src/main/java/.../domain/PlatformConfig.java`
  - `src/main/resources/platforms/*.json`

## 关联模块

- AdaptAgent：读取 `stylePrompt` 和平台能力生成内容。
- PublishAgent：发布前校验标题、正文、标签和图片数量。
- 前端工作台：动态渲染平台 Tab 和表单限制。

## 编写规范

- 平台标识统一小写。
- `config_json` 用 CLOB 保存完整 JSON，避免过早拆成大量列。
- 启动同步配置时支持覆盖更新，不重复插入同平台。
- 默认查询只返回 `enabled=true` 的平台。
- 配置解析失败必须显式报错，不静默跳过。
- 不写发布记录状态流转、媒体磁盘写入、LLM 调用或 Mock 页面。

## 接口边界

- `GET /api/configs/platforms`
- `GET /api/configs/platforms/{platform}`

## Agent.md 更新

每 3 轮后更新本文件和根目录 `agent.md`，补充新增平台、规则字段、配置文件路径和调用方变化。
