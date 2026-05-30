# ConfigAgent Module Guide

## 模块功能

ConfigAgent 负责平台规则配置。它把各平台的字数、标签、图片、格式能力和风格提示沉淀成可查询规则，供前端、适配和发布模块复用。

## 启动 Hook

```bash
rtk python hooks/config_hook.py
```

若 Hook 提示 3 轮限制，先刷新根 `agent.md` 和本文件，再重新运行 Hook。

## 源码与测试导航

- `src/main/java/com/example/pulsedistro/controller/PlatformConfigController.java`
- `src/main/java/com/example/pulsedistro/service/PlatformConfigService.java`
- `src/main/java/com/example/pulsedistro/domain/PlatformConfig.java`
- `src/main/java/com/example/pulsedistro/model/PlatformRule.java`
- `src/main/java/com/example/pulsedistro/dto/config/*`
- `src/main/resources/platforms/xiaohongshu.json`
- `src/main/resources/platforms/zhihu.json`
- `src/main/resources/platforms/wechat.json`
- `src/main/resources/platforms/bilibili.json`
- `src/test/java/com/example/pulsedistro/config/*`

## API 边界

- `GET /api/configs/platforms`
- `GET /api/configs/platforms/{platform}`

默认查询只返回 `enabled=true` 的平台。

## 数据模型

- `PlatformConfig`
  - `id`
  - `platform`
  - `displayName`
  - `configJson`
  - `enabled`
  - `createdAt`
- `PlatformRule`
  - `platform`
  - `displayName`
  - `maxTitleLength`
  - `maxContentLength`
  - `maxTags`
  - `supportsMarkdown`
  - `image.maxCount`
  - `image.acceptedMimeTypes`
  - `stylePrompt`

## 关联模块

- AdaptAgent：读取 `stylePrompt`、字数限制和标签规则生成内容。
- PublishAgent：发布前校验标题、正文、标签和图片数量。
- 前端工作台：动态渲染平台 Tab、字数限制、标签限制和图片限制。

## 编写规范

- 平台标识统一小写：`xiaohongshu`、`zhihu`、`wechat`、`bilibili`。
- `configJson` 用 CLOB 保存完整 JSON，避免过早拆成大量列。
- 启动同步配置时支持覆盖更新，不重复插入同平台。
- 配置解析失败必须显式报错，不静默跳过。
- 只写配置加载和查询，不写发布记录状态流转、媒体磁盘写入、LLM 调用或 Mock 页面。

## 配置变量

- 默认不直接读取 `.env` 隐私变量。
- 若未来支持外部配置目录，可新增 `PLATFORM_CONFIG_ROOT`，但 MVP 优先使用 classpath `platforms/*.json`。

## 2026-05-30 Round 2 Fix Sync

- Runtime executor sizing is environment-mapped through Spring properties: `ADAPT_EXECUTOR_CORE_SIZE`, `ADAPT_EXECUTOR_MAX_SIZE`, and `ADAPT_EXECUTOR_QUEUE_CAPACITY`.
- ConfigAgent owns only the configuration boundary; AdaptAgent consumes the executor bean for async work.
