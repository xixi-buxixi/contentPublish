# 多平台内容自动适配与分发工具（Pulse Distro）

## 需求规格说明书与可行实现方案

| 版本 | 修订日期 | 状态 | 作者 | 变更说明 |
| ---- | -------- | ---- | ---- | -------- |
| v1.1.0 | 2026-05-29 | 优化稿 | 独立开发者 | 补充模拟发布、MVP 边界、接口设计、数据模型与平台扩展架构 |

## 1. 题目匹配与产品定位

### 1.1 题目要求拆解

题目二要求设计并实现一个帮助创作者提升多平台发布效率的工具，核心能力包括：

1. 用户在工具中输入内容。
2. 系统自动适配公众号、知乎、B站、小红书等平台的格式与内容风格。
3. 支持一键发布，允许采用模拟发布方式完成演示。
4. 给出后续扩展更多平台的架构设计。

本方案围绕上述四点设计，默认以“模拟发布”作为稳定可演示路径，将“真实浏览器辅助发布”作为高级扩展能力，避免因平台验证码、风控和页面改版导致演示不可控。

### 1.2 产品定位

Pulse Distro 是一款面向自媒体创作者的跨平台内容适配与分发效率工具。用户只需要输入一份原始内容，系统即可生成适合不同平台的发布版本，并在统一工作台中完成预览、人工微调、模拟发布和发布记录管理。

产品目标不是替代创作者判断，而是减少重复排版、复制粘贴和平台格式适配成本，让创作者能把主要精力放在内容质量上。

### 1.3 MVP 交付范围

为保证作品可实现、可演示、可扩展，MVP 优先实现以下能力：

| 模块 | MVP 实现 | 说明 |
| ---- | -------- | ---- |
| 内容输入 | 支持粘贴文本、上传 Markdown/TXT/DOCX | DOCX 可先解析纯文本，复杂样式作为后续增强 |
| 平台适配 | 生成公众号、知乎、B站、小红书四个平台版本 | 采用“规则模板 + LLM 改写”混合策略 |
| 内容预览 | 支持多平台 Tab 预览和人工编辑 | 用户可在发布前调整标题、正文、标签 |
| 一键发布 | 默认执行模拟发布 | 写入发布记录，生成模拟平台链接 |
| 发布记录 | 展示平台、状态、发布时间、发布链接 | 用于证明完整流程闭环 |
| 平台扩展 | 提供配置文件和统一适配器接口 | 新平台不需要修改核心调度逻辑 |

高级能力包括 Chrome 插件真实填表发布、验证码人工接管、发布后视觉校验等，可作为系统演进方向，不作为 MVP 的强依赖。

## 2. 总体架构

系统采用“核心调度内核 + 平台适配插件 + 发布执行器”的分层架构。核心服务只负责内容流转和任务状态，不直接绑定具体平台规则。

```text
┌─────────────────────────────────────────────────────────────┐
│ Web 控制台                                                   │
│ 内容输入 / 平台预览 / 人工编辑 / 一键模拟发布 / 发布记录       │
└───────────────────────────▲─────────────────────────────────┘
                            │ REST + WebSocket
                            ▼
┌─────────────────────────────────────────────────────────────┐
│ Java Spring Boot 核心服务                                     │
│ TaskService / AdaptService / PublishService / MediaService   │
│ PluginManager / RecordService / 平台配置加载 / 状态机          │
└───────────────┬───────────────────────────────┬─────────────┘
                │                               │
                ▼                               ▼
┌──────────────────────────────┐     ┌────────────────────────┐
│ Python FastAPI 内容适配服务   │     │ 发布执行器              │
│ LLM 改写 / 规则格式化 / 校验   │     │ MockPublisher 默认模式   │
└──────────────────────────────┘     │ LocalChromePublisher 可选│
                                      └────────────────────────┘
                ▲
                │ 平台插件配置
                ▼
┌─────────────────────────────────────────────────────────────┐
│ resources/platforms/*.json + media storage                    │
│ 平台能力、格式规则、标题策略、标签策略、发布字段映射、图片 URL │
└─────────────────────────────────────────────────────────────┘
```

该架构的优点是演示链路稳定，扩展边界清晰。即使真实平台发布暂时不可用，系统仍然能够完整展示“输入、适配、预览、发布、记录”的主流程。

## 3. 核心功能设计

### 3.1 内容输入与标准化解析

用户可以粘贴正文，也可以上传 Markdown、TXT、DOCX 文件。后端将不同输入统一转换为内部标准内容模型，便于后续平台适配。

```json
{
  "title": "原始标题",
  "summary": "内容摘要",
  "blocks": [
    { "type": "heading", "level": 2, "text": "小标题" },
    { "type": "paragraph", "text": "正文段落" },
    {
      "type": "image",
      "mediaId": 501,
      "url": "https://cdn.example.com/media/501.png",
      "alt": "配图说明"
    }
  ],
  "metadata": {
    "author": "creator",
    "sourceType": "markdown"
  }
}
```

MVP 阶段可以优先支持标题、段落、列表、图片四类元素，复杂表格、脚注、公式等作为后续增强。

图片不能停留在 `local://image-1.png` 这类前端临时路径。后端接收图片后，需要通过 `MediaService` 统一落库和存储，并把标准化内容中的图片引用替换为 Web 可访问 URL。MVP 可以先使用本地磁盘存储，例如 `/uploads/{taskId}/{fileName}`，对外提供 `/media/{mediaId}` 访问地址；生产环境再切换为 OSS、S3、COS 等对象存储。

### 3.2 多平台内容适配

系统根据平台能力和内容风格生成不同发布版本。平台适配不只处理格式，还会调整表达方式、标题风格、标签和摘要。

| 平台 | 适配目标 | 输出内容 |
| ---- | -------- | -------- |
| 微信公众号 | 适合长文阅读，结构清晰，摘要明确 | 标题候选、摘要、HTML 正文、封面建议 |
| 知乎 | 专业、论证充分，保留 Markdown 结构 | 问答式标题、Markdown 正文、参考链接 |
| B站 | 更适合专栏或动态引流 | 专栏正文、动态短文、视频引流文案 |
| 小红书 | 更短、更口语化，强调标签和种草表达 | 爆款标题、短正文、Emoji 建议、话题标签 |

适配策略采用两层设计：

1. 规则层：处理字数限制、标题长度、标签数量、Markdown/HTML 转换等确定性规则。
2. 智能层：调用 LLM 进行语言风格改写、摘要提取和标题生成。

这样既能保证格式稳定，又能体现不同平台的内容风格差异。

### 3.3 多平台预览与人工微调

Web 控制台提供多平台 Tab 预览：

1. 原始内容。
2. 微信公众号版本。
3. 知乎版本。
4. B站版本。
5. 小红书版本。

每个平台版本都允许用户二次编辑。发布时以用户最终确认后的版本为准，避免 AI 改写直接发布带来的语义偏差。

### 3.4 一键发布与模拟发布

MVP 默认采用模拟发布，保证演示稳定可控。用户点击“一键发布”后，系统为每个平台创建发布任务，并执行以下流程：

1. 校验该平台内容是否满足字数、标题、标签等限制。
2. 校验正文中的图片 `mediaId` 是否都有可访问的 `public_url`。
3. 写入发布记录表。
4. 生成模拟发布链接，例如 `/mock/xiaohongshu/{recordId}`。
5. 在工作台中展示发布状态、发布时间和模拟链接。

模拟发布页面展示最终发布效果，用于证明内容已经成功完成平台适配和分发流程。为了增强演示表现力，Mock 页面不只展示普通文章详情，而是按平台渲染拟态界面：小红书使用手机壳卡片、头像、Emoji 文案、话题标签和评论占位；知乎使用 PC 端问答/专栏布局；公众号使用移动端长文排版；B站使用专栏或动态样式。这样能让评审直观看到“同一份内容被适配成不同平台形态”。

### 3.5 真实发布扩展

真实发布作为高级模式，可以通过本地 Chrome 插件完成辅助填表：

1. 插件运行在用户本地浏览器，复用用户已有登录态。
2. 后端不保存平台账号、密码、Cookie 或 Token。
3. 插件根据平台脚本定位标题、正文、标签、图片上传等输入区域。
4. 遇到验证码、二次确认、平台风控时，任务进入人工接管状态。
5. 发布按钮建议由用户最终确认，降低违规自动化风险。

真实发布前必须检查插件在线状态。Chrome 插件启动或激活时向后端发送注册事件，后端由 `PluginManager` 维护 `userId -> pluginSession` 的在线映射和最近心跳时间。发布执行器路由时先执行校验：

```text
if (mode == "real" && !PluginManager.isOnline(userId)) {
  return "请先开启您的浏览器分发插件";
}
```

图片上传也必须走远程 URL。插件不能读取后端内部的 `local://` 临时路径，应从后端提供的 `public_url` 获取图片，使用 `fetch(imageUrl)` 下载为 `Blob/File`，再通过平台页面的文件输入框或拖拽上传逻辑注入。

真实发布存在平台 UI 改版、验证码、账号风控、接口限制等不可控因素，因此不承诺百分百成功率。系统通过模拟发布和人工接管机制保证核心流程可用。

## 4. 任务状态机

发布任务以平台为粒度独立流转，同一篇内容可以同时生成多个平台任务。

```text
PENDING
  -> ADAPTING
  -> READY
  -> PUBLISHING
  -> SUCCESS
  -> VERIFIED_SUCCESS

PUBLISHING -> SUSPENDED -> READY
PUBLISHING -> FAILED
READY -> SKIPPED
SUCCESS -> WARN
```

状态说明：

| 状态 | 含义 |
| ---- | ---- |
| PENDING | 任务已创建，等待内容解析 |
| ADAPTING | 正在进行平台内容适配 |
| READY | 内容已生成，等待发布 |
| PUBLISHING | 正在执行模拟发布或真实发布 |
| SUCCESS | 发布执行成功 |
| VERIFIED_SUCCESS | 发布后校验通过 |
| WARN | 发布成功但校验存在风险，例如排版异常或疑似内容缺失 |
| SUSPENDED | 真实发布遇到验证码或人工确认点 |
| FAILED | 发布失败且无法自动恢复 |
| SKIPPED | 用户主动跳过该平台 |

## 5. 数据模型设计

### 5.1 内容任务表 content_task

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| title | varchar | 原始标题 |
| source_type | varchar | text、markdown、docx |
| raw_content | text | 原始内容 |
| normalized_content | json | 标准化内容模型，图片只保存 `mediaId` 与 `publicUrl` 引用 |
| cover_media_id | bigint | 可选封面图资源 ID |
| status | varchar | 总任务状态 |
| created_at | datetime | 创建时间 |
| updated_at | datetime | 更新时间 |

### 5.2 平台发布记录表 platform_publish_record

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| task_id | bigint | 所属内容任务 |
| platform | varchar | 平台标识，如 wechat、zhihu、bilibili、xiaohongshu |
| adapted_title | varchar | 平台适配后的标题 |
| adapted_content | text | 平台适配后的正文 |
| adapted_media_json | json | 平台适配后的图片顺序、封面图、插图锚点 |
| publish_mode | varchar | mock 或 real |
| status | varchar | 平台任务状态 |
| publish_url | varchar | 模拟或真实发布链接 |
| error_message | varchar | 失败原因 |
| created_at | datetime | 创建时间 |
| published_at | datetime | 发布时间 |

### 5.3 媒体资源表 media_resource

媒体资源必须独立建模，避免正文 JSON 中的图片路径失效。所有图片上传后先进入 `media_resource`，再由标准化内容和平台发布记录引用。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| task_id | bigint | 所属内容任务 |
| original_name | varchar | 用户上传时的文件名 |
| mime_type | varchar | image/png、image/jpeg 等 |
| size_bytes | bigint | 文件大小 |
| width | int | 图片宽度 |
| height | int | 图片高度 |
| storage_type | varchar | local、oss、s3、cos |
| storage_key | varchar | 存储对象 Key 或本地相对路径 |
| public_url | varchar | 前端、Mock 页面和 Chrome 插件可访问的 URL |
| sha256 | varchar | 文件哈希，用于去重和缓存 |
| status | varchar | UPLOADED、READY、FAILED |
| created_at | datetime | 创建时间 |

媒体处理策略：

1. 模拟发布：Mock 页面直接使用 `public_url` 渲染图片。
2. 真实发布：后端把 `public_url` 下发给 Chrome 插件，插件通过 `fetch(publicUrl)` 下载为 `Blob/File` 后上传。
3. 本地开发：`public_url` 可以是 `http://localhost:8080/media/{mediaId}`。
4. 生产部署：`public_url` 建议改为对象存储 CDN 地址。

### 5.4 平台配置表 platform_config

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| platform | varchar | 平台唯一标识 |
| display_name | varchar | 平台名称 |
| config_json | json | 字数、格式、标签、发布字段映射等配置 |
| enabled | boolean | 是否启用 |

### 5.5 插件会话表 plugin_session

真实发布需要知道用户本地 Chrome 插件是否在线。`plugin_session` 可持久化最近一次绑定状态，运行时再由 `PluginManager` 维护内存在线表。

| 字段 | 类型 | 说明 |
| ---- | ---- | ---- |
| id | bigint | 主键 |
| user_id | bigint | 用户 ID |
| session_id | varchar | 插件会话 ID |
| extension_version | varchar | 插件版本 |
| browser | varchar | 浏览器信息 |
| status | varchar | ONLINE、OFFLINE |
| last_heartbeat_at | datetime | 最近心跳时间 |
| created_at | datetime | 创建时间 |

## 6. 接口设计

### 6.1 创建内容任务

```http
POST /api/tasks
Content-Type: application/json
```

```json
{
  "title": "如何提升内容分发效率",
  "sourceType": "markdown",
  "content": "# 标题\n正文内容..."
}
```

返回任务 ID 和标准化内容。若用户同时上传图片，可采用 `multipart/form-data` 或单独的媒体上传接口；后端需要先生成 `media_resource` 记录，再把正文中的图片占位符替换为 `mediaId`。

### 6.2 异步触发平台适配

```http
POST /api/tasks/{taskId}/adapt
Content-Type: application/json
```

```json
{
  "platforms": ["wechat", "zhihu", "bilibili", "xiaohongshu"]
}
```

该接口不应同步等待所有 LLM 结果。后端收到请求后立即创建各平台适配任务，返回 `202 Accepted`，前端通过 WebSocket 接收增量结果。

```json
{
  "taskId": 10001,
  "status": "PROCESSING",
  "message": "平台适配任务已开始，请通过 WebSocket 接收生成结果。"
}
```

这样可以避免 10 到 15 秒的 HTTP 阻塞，也能让控制台呈现“小红书生成成功 -> 知乎生成成功 -> 公众号生成成功”的动态流水线体验。

### 6.3 保存人工修改

```http
PUT /api/tasks/{taskId}/records/{recordId}
Content-Type: application/json
```

```json
{
  "adaptedTitle": "适配后的标题",
  "adaptedContent": "用户确认后的正文",
  "tags": ["效率工具", "内容创作"]
}
```

### 6.4 一键模拟发布

```http
POST /api/tasks/{taskId}/publish
Content-Type: application/json
```

```json
{
  "mode": "mock",
  "platforms": ["wechat", "zhihu", "bilibili", "xiaohongshu"]
}
```

返回每个平台的发布状态和模拟链接。

如果 `mode` 为 `real`，发布服务需要先检查插件在线态：

```json
{
  "mode": "real",
  "platforms": ["zhihu"]
}
```

当插件未在线时，接口直接返回可理解错误：

```json
{
  "status": "FAILED",
  "message": "请先开启您的浏览器分发插件"
}
```

### 6.5 WebSocket 状态推送

平台适配完成时，后端向控制台推送平台级结果：

```json
{
  "event": "PLATFORM_ADAPT_COMPLETED",
  "timestamp": 1779954061000,
  "data": {
    "taskId": 10001,
    "recordId": 90001,
    "platform": "xiaohongshu",
    "status": "READY",
    "adaptedTitle": "3 步搞定多平台内容分发",
    "adaptedContent": "适配后的正文...",
    "media": [
      {
        "mediaId": 501,
        "publicUrl": "http://localhost:8080/media/501"
      }
    ]
  }
}
```

发布状态变化时，推送发布事件：

```json
{
  "event": "PUBLISH_STATUS_CHANGED",
  "timestamp": 1779954061000,
  "data": {
    "taskId": 10001,
    "recordId": 90001,
    "platform": "xiaohongshu",
    "status": "SUCCESS",
    "publishUrl": "/mock/xiaohongshu/90001"
  }
}
```

插件启动和心跳事件也走同一条 WebSocket 或 REST 通道：

```json
{
  "event": "PLUGIN_REGISTERED",
  "timestamp": 1779954061000,
  "data": {
    "userId": 1,
    "sessionId": "plugin-session-001",
    "extensionVersion": "1.0.0",
    "status": "ONLINE"
  }
}
```

## 7. 平台扩展架构

为了支持更多平台，系统将平台差异封装到配置和适配器中。核心服务只依赖统一接口，不关心具体平台实现。

### 7.1 平台能力配置

示例：`resources/platforms/xiaohongshu.json`

```json
{
  "platform": "xiaohongshu",
  "displayName": "小红书",
  "maxTitleLength": 20,
  "maxContentLength": 1000,
  "supportsMarkdown": false,
  "supportsHtml": false,
  "maxTags": 10,
  "image": {
    "maxCount": 9,
    "acceptedMimeTypes": ["image/jpeg", "image/png", "image/webp"],
    "uploadStrategy": "remoteUrlToBlob"
  },
  "stylePrompt": "将内容改写为口语化、轻量、适合种草分享的风格。",
  "publishFields": {
    "title": "title",
    "content": "content",
    "tags": "tags",
    "images": "images"
  }
}
```

### 7.2 统一适配器接口

```java
public interface PlatformAdapter {
    String platform();

    PlatformCapability capability();

    AdaptedContent adapt(NormalizedContent content, PlatformConfig config);

    ValidationResult validate(AdaptedContent content, PlatformConfig config);
}
```

### 7.3 统一发布器接口

```java
public interface Publisher {
    PublishMode mode();

    PublishResult publish(PublishContext context, AdaptedContent content, PlatformConfig config);
}
```

MVP 提供 `MockPublisher`，用于稳定演示。真实发布扩展可提供 `LocalChromePublisher`，由本地浏览器插件执行具体填表动作。`PublishContext` 至少应包含 `userId`、`publishMode`、`mediaResources`、`pluginSession`，这样发布器可以在进入真实发布前校验插件在线态和图片 URL 可访问性。

### 7.4 新增平台步骤

以新增 Medium 为例：

1. 新增 `resources/platforms/medium.json`，声明平台字数、格式能力、标签能力和风格提示词。
2. 新增 `MediumAdapter` 或通过通用 LLM 适配器读取配置生成内容。
3. 若只支持模拟发布，无需新增浏览器脚本。
4. 若支持真实发布，再新增 Chrome 插件脚本 `src/inject/medium.js`。
5. 在平台配置表中启用 `medium`，核心任务流不需要修改。

## 8. 发布后校验

MVP 阶段可以先做结构化校验：

1. 标题是否为空。
2. 正文是否为空。
3. 字数是否超过平台限制。
4. 标签数量是否超过平台限制。
5. 图片资源是否都存在 `media_resource` 记录并具备可访问 URL。
6. 模拟发布页面是否能正常访问。

高级阶段可以加入 Playwright 截图和视觉模型校验，用于发现真实页面中的排版错位、图片缺失、内容被平台折叠或屏蔽等问题。

## 9. 可行性分析

### 9.1 技术可行性

内容输入、格式解析、平台适配、模拟发布和发布记录管理都可以通过常规 Web 应用实现，技术风险较低。LLM 改写可以通过服务化接口接入，也可以在无模型环境下先使用规则模板生成演示结果。

真实发布的技术风险较高，主要来自平台页面变化、验证码、风控策略和账号安全限制。因此真实发布应设计为可选能力，并提供人工确认和人工接管。

### 9.2 合规与安全可行性

系统不保存第三方平台账号、密码、Cookie 或 Token。真实发布模式下，登录态只存在于用户本地浏览器。工具界面应明确提示：本工具用于辅助内容整理和发布，用户需要遵守各平台规则，避免高频、批量、违规发布。

### 9.3 演示可行性

模拟发布可以完整展示题目要求的主流程：

```text
输入内容 -> 自动适配多平台 -> 人工预览编辑 -> 一键模拟发布 -> 查看发布记录和模拟链接
```

该链路不依赖真实平台账号，适合课堂答辩、项目验收和离线演示。

## 10. 优化后的实现优先级

### 第一阶段：可演示 MVP

1. 完成内容输入页面。
2. 完成四个平台的适配模板。
3. 完成多平台预览和人工编辑。
4. 完成一键模拟发布。
5. 完成发布记录列表和模拟发布详情页。

### 第二阶段：智能增强

1. 接入 LLM 进行标题生成、摘要提取和风格改写。
2. 加入平台规则校验和风险提示。
3. 支持 DOCX 图片提取和 Markdown/HTML 转换。

### 第三阶段：真实发布扩展

1. 开发本地 Chrome 插件。
2. 支持用户本地登录态辅助填表。
3. 加入验证码人工接管。
4. 加入发布后页面截图校验。

## 11. 总结

优化后的 Pulse Distro 更贴合题目要求：既能体现多平台自动适配和一键发布能力，又通过模拟发布保证作品可落地、可演示、可验收。同时，系统保留平台插件、统一适配器和统一发布器设计，为未来扩展更多平台和接入真实发布能力留出了清晰边界。
