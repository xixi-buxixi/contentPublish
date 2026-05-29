# Pulse Distro Java 轻量化接口文档

## 版本说明

| 版本 | 修订日期 | 状态 | 说明 |
| ---- | -------- | ---- | ---- |
| v1.1.0 | 2026-05-29 | 优化稿 | 对齐 Java 轻量化实践方案，补齐媒体资源、异步适配、Mock 发布、WebSocket、插件在线态和兜底查询接口 |

## 1. 可行性检查结论

本接口适合 `Spring Boot 3.3 + H2 + 本地媒体存储 + WebSocket + LangChain4j/模板降级` 的轻量单体实现。

## 2. 总体约定

### 2.1 基础地址

```text
http://localhost:8080/api
```

非 API 页面和媒体访问：

```text
http://localhost:8080/media/{mediaId}
http://localhost:8080/mock/{platform}/{recordId}
ws://localhost:8080/ws/pipeline?userToken={userToken}&traceId={traceId}
```

### 2.2 Content-Type

普通 JSON 接口：

```http
Content-Type: application/json
```

媒体上传接口：

```http
Content-Type: multipart/form-data
```

### 2.3 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

建议状态码：

本项目采用前端友好的统一 JSON 方案：普通 `/api` 接口在进入 Controller 且能构造响应时，HTTP 状态默认返回 `200`，业务状态通过 JSON 中的 `code` 区分。这样 Axios 不会因为可预期的业务错误直接进入 `catch`。只有媒体流、Mock HTML、静态资源、请求未命中 Controller 或系统级异常，才使用真实 HTTP `404/500`。

| code | HTTP 状态 | 含义 |
| ---- | --------- | ---- |
| 200 | 200 | 请求成功 |
| 202 | 200 | 异步任务已接受 |
| 400 | 200 | 参数错误或业务校验失败 |
| 404 | 200 | API 资源不存在 |
| 409 | 200 | 状态冲突，例如重复发布、记录未就绪 |
| 500 | 200 | 系统异常已被统一异常处理器捕获 |

错误示例：

```json
{
  "code": 400,
  "message": "请先开启您的浏览器分发插件",
  "data": {
    "reason": "PLUGIN_OFFLINE"
  }
}
```

### 2.4 会话隔离约定

轻量 MVP 可以不做完整登录，但必须给每个工作台分配一个 `userToken` 和 `traceId`，避免多人演示时互相收到 WebSocket 事件或覆盖插件会话。

推荐工作台首次打开时调用：

```http
POST /api/session/init
Content-Type: application/json
```

```json
{
  "clientType": "web",
  "nickname": "demo-user"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userId": 1,
    "userToken": "ut_8f3c9a_demo",
    "traceId": "trace_20260529_001"
  }
}
```

后续普通 API 建议统一携带：

```http
X-User-Token: ut_8f3c9a_demo
X-Trace-Id: trace_20260529_001
```

`userId` 仅用于本地演示和数据库关联，不作为鉴权来源。Chrome 插件注册、心跳和真实发布也必须使用同一个 `userToken`，后端才能确认“Web 工作台”和“本地插件”属于同一会话。

### 2.5 枚举约定

平台标识统一使用小写：

```text
xiaohongshu, zhihu, wechat, bilibili
```

发布模式统一使用小写：

```text
mock, real
```

任务状态：

```text
PENDING, ADAPTING, READY, PUBLISHING, SUCCESS, VERIFIED_SUCCESS, WARN, SUSPENDED, FAILED, SKIPPED
```

## 3. 内容任务模块

### 3.1 创建内容任务

```http
POST /api/tasks
Content-Type: application/json
```

说明：创建一篇待分发内容任务。MVP 阶段可以只传纯文本或 Markdown；图片建议通过媒体接口上传后再写入标准内容模型。

请求：

```json
{
  "title": "如何高效进行多平台内容分发",
  "sourceType": "MARKDOWN",
  "rawContent": "# 核心观点\n别再手动复制了..."
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1001,
    "status": "PENDING",
    "createdAt": "2026-05-29T17:10:20"
  }
}
```

### 3.2 获取任务详情

```http
GET /api/tasks/{taskId}
```

说明：用于页面刷新、WebSocket 断线恢复、任务详情展示。

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1001,
    "title": "如何高效进行多平台内容分发",
    "sourceType": "MARKDOWN",
    "status": "READY",
    "createdAt": "2026-05-29T17:10:20",
    "updatedAt": "2026-05-29T17:12:00"
  }
}
```

### 3.3 获取标准内容模型

```http
GET /api/tasks/{taskId}/normalized
```

说明：获取解析后的 `NormalizedContent`，包含文本块和图片引用。

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "title": "如何高效进行多平台内容分发",
    "summary": "别再手动复制了，内容分发这样做更快。",
    "blocks": [
      {
        "type": "heading",
        "level": 1,
        "text": "核心观点",
        "media": null
      },
      {
        "type": "image",
        "level": null,
        "text": null,
        "media": {
          "mediaId": 501,
          "publicUrl": "http://localhost:8080/media/501",
          "alt": "架构图",
          "width": 1200,
          "height": 800
        }
      }
    ]
  }
}
```

### 3.4 更新标准内容模型

```http
PUT /api/tasks/{taskId}/normalized
Content-Type: application/json
```

说明：前端插入图片、调整段落或重新组织正文后，将最终标准内容模型回写。适配接口应以该模型为准。

一致性要求：

1. 后端只信任 `mediaId`，`publicUrl` 仅作为前端展示冗余字段；保存时应根据 `mediaId` 从 `media_resource` 重新查询并覆盖。
2. 所有 `mediaId` 必须属于当前 `taskId`，且状态为 `READY`。
3. 标准内容模型中的媒体引用会影响删除校验。只要 `normalized_content_json` 中仍引用该 `mediaId`，就不允许删除媒体文件。

请求：

```json
{
  "title": "如何高效进行多平台内容分发",
  "summary": "别再手动复制了，内容分发这样做更快。",
  "blocks": [
    {
      "type": "paragraph",
      "text": "这是正文第一段。",
      "level": null,
      "media": null
    },
    {
      "type": "image",
      "text": null,
      "level": null,
      "media": {
        "mediaId": 501,
        "publicUrl": "http://localhost:8080/media/501",
        "alt": "架构图"
      }
    }
  ]
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1001,
    "status": "PENDING"
  }
}
```

## 4. 媒体资源模块

### 4.1 上传媒体文件

```http
POST /api/tasks/{taskId}/media
Content-Type: multipart/form-data
```

说明：为指定任务上传图片。后端保存到本地目录，写入 `media_resource`，并返回浏览器可访问的 `publicUrl`。

表单参数：

| 参数 | 类型 | 必填 | 说明 |
| ---- | ---- | ---- | ---- |
| file | file | 是 | 图片文件 |
| alt | string | 否 | 图片说明 |

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "mediaId": 501,
    "publicUrl": "http://localhost:8080/media/501",
    "mimeType": "image/png",
    "sizeBytes": 204857,
    "width": 1200,
    "height": 800,
    "status": "READY"
  }
}
```

### 4.2 获取任务媒体列表

```http
GET /api/tasks/{taskId}/media
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "mediaId": 501,
      "originalName": "cover.png",
      "publicUrl": "http://localhost:8080/media/501",
      "mimeType": "image/png",
      "width": 1200,
      "height": 800,
      "status": "READY"
    }
  ]
}
```

### 4.3 访问媒体文件

```http
GET /media/{mediaId}
```

说明：该地址不带 `/api` 前缀，直接返回图片二进制流，供 Mock 页面和 Chrome 插件访问。

响应：

```http
Content-Type: image/png
```

### 4.4 删除媒体文件

```http
DELETE /api/tasks/{taskId}/media/{mediaId}
```

说明：仅允许删除当前任务下未被标准内容模型且未被发布记录引用的媒体。后端必须同时检查：

1. `content_task.normalized_content_json` 是否仍包含该 `mediaId`。
2. `platform_publish_record.adapted_media_json` 是否仍包含该 `mediaId`。
3. 待删除媒体是否属于当前 `taskId`。

若任一引用存在，返回业务 `code=409`，提示前端先从正文或发布记录中移除该图片。

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "mediaId": 501,
    "deleted": true
  }
}
```

引用冲突响应：

```json
{
  "code": 409,
  "message": "媒体仍被标准内容或发布记录引用，无法删除",
  "data": {
    "mediaId": 501,
    "referencedBy": ["normalizedContent", "publishRecord:9001"]
  }
}
```

## 5. 平台适配模块

### 5.1 异步触发平台适配

```http
POST /api/tasks/{taskId}/adapt
Content-Type: application/json
```

说明：异步调用 LangChain4j 或模板降级适配器。接口只负责触发任务，不同步等待所有平台结果。

关键约束：后端必须在同步返回前先为每个平台初始化 `platform_publish_record`，状态置为 `ADAPTING`，并把 `recordId` 返回给前端。这样即使前端刷新页面，也能通过 `GET /api/tasks/{taskId}/records` 恢复每个平台的 Loading 状态。

请求：

```json
{
  "platforms": ["xiaohongshu", "zhihu", "wechat", "bilibili"],
  "forceRegenerate": false
}
```

响应：

```json
{
  "code": 202,
  "message": "accepted",
  "data": {
    "taskId": 1001,
    "status": "ADAPTING",
    "records": [
      {
        "platform": "xiaohongshu",
        "recordId": 9001,
        "status": "ADAPTING"
      },
      {
        "platform": "zhihu",
        "recordId": 9002,
        "status": "ADAPTING"
      },
      {
        "platform": "wechat",
        "recordId": 9003,
        "status": "ADAPTING"
      },
      {
        "platform": "bilibili",
        "recordId": 9004,
        "status": "ADAPTING"
      }
    ],
    "message": "平台适配已开始，请监听 WebSocket。"
  }
}
```

### 5.2 获取平台发布记录列表

```http
GET /api/tasks/{taskId}/records
```

说明：用于恢复页面状态、展示所有平台适配结果和发布状态。

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "recordId": 9001,
      "platform": "xiaohongshu",
      "adaptedTitle": "别再手动复制了，内容分发这样做更快",
      "status": "READY",
      "publishMode": null,
      "publishUrl": null
    }
  ]
}
```

### 5.3 获取单条发布记录详情

```http
GET /api/records/{recordId}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "recordId": 9001,
    "taskId": 1001,
    "platform": "xiaohongshu",
    "adaptedTitle": "别再手动复制了，内容分发这样做更快",
    "adaptedContent": "亲爱的家人们！今天分享一个超绝的分发方案... ✨",
    "tags": ["搞定分发", "独立开发"],
    "media": [
      {
        "mediaId": 501,
        "publicUrl": "http://localhost:8080/media/501",
        "alt": "架构图"
      }
    ],
    "styleExplanation": "已增加 Emoji 并转化为小红书口语化种草风。",
    "status": "READY"
  }
}
```

### 5.4 保存人工编辑后的平台内容

```http
PUT /api/records/{recordId}
Content-Type: application/json
```

说明：用户在工作台中修改标题、正文、标签和图片顺序后回写。发布时以该版本为准。

请求：

```json
{
  "adaptedTitle": "修改后的小红书标题",
  "adaptedContent": "修改后的爆款文案 🚀 #搞定分发",
  "tags": ["小红书", "自媒体", "副业"],
  "mediaIds": [501]
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "recordId": 9001,
    "status": "READY"
  }
}
```

### 5.5 跳过某个平台

```http
POST /api/records/{recordId}/skip
```

说明：用户不想发布某个平台时，将记录状态置为 `SKIPPED`。

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "recordId": 9001,
    "status": "SKIPPED"
  }
}
```

## 6. 发布与 Mock 模块

### 6.1 一键发布

```http
POST /api/tasks/{taskId}/publish
Content-Type: application/json
X-User-Token: ut_8f3c9a_demo
X-Trace-Id: trace_20260529_001
```

说明：触发一键模拟发布或真实发布。MVP 默认使用 `mock`。当 `mode=real` 时，后端必须校验当前请求的 `X-User-Token` 与在线插件会话绑定的 `userToken` 一致，避免多个演示用户互相覆盖插件会话。

请求：

```json
{
  "mode": "mock",
  "platforms": ["xiaohongshu", "zhihu"],
  "clientSessionId": "web-session-001"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "taskId": 1001,
    "mode": "mock",
    "results": [
      {
        "recordId": 9001,
        "platform": "xiaohongshu",
        "status": "SUCCESS",
        "publishUrl": "http://localhost:8080/mock/xiaohongshu/9001"
      },
      {
        "recordId": 9002,
        "platform": "zhihu",
        "status": "SUCCESS",
        "publishUrl": "http://localhost:8080/mock/zhihu/9002"
      }
    ]
  }
}
```

真实发布插件未在线时：

```json
{
  "code": 400,
  "message": "请先开启您的浏览器分发插件",
  "data": {
    "reason": "PLUGIN_OFFLINE",
    "mode": "real",
    "userToken": "ut_8f3c9a_demo"
  }
}
```

### 6.2 Mock 拟态页面

```http
GET /mock/{platform}/{recordId}
```

说明：该地址不带 `/api` 前缀，适用于后端 Controller 或 Thymeleaf 直接返回 HTML 页面。

如果项目采用 Vue 纯前端路由渲染 Mock 页面，建议不要让后端占用 `/mock/{platform}/{recordId}` 路由，而是使用：

```text
http://localhost:8080/#/mock/{platform}/{recordId}
```

此时后端只保留 `GET /api/mock/{platform}/{recordId}` 作为 JSON 数据接口，避免前端路由和后端路由冲突。

建议表现：

| 平台 | Mock 页面表现 |
| ---- | ------------- |
| xiaohongshu | 手机壳容器、图文卡片、Emoji 文案、话题标签、评论占位 |
| zhihu | PC 问答/专栏布局、作者信息、Markdown 正文 |
| wechat | 移动端公众号长文排版、封面图、摘要 |
| bilibili | 专栏或动态样式、封面、互动按钮占位 |

### 6.3 获取 Mock 页面数据

```http
GET /api/mock/{platform}/{recordId}
```

说明：如果 Mock 页面由 Vue 前端路由渲染，可以调用该 JSON 接口获取数据。

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "recordId": 9001,
    "platform": "xiaohongshu",
    "title": "修改后的小红书标题",
    "content": "修改后的爆款文案 🚀 #搞定分发",
    "tags": ["小红书", "自媒体", "副业"],
    "media": [
      {
        "mediaId": 501,
        "publicUrl": "http://localhost:8080/media/501"
      }
    ],
    "publishedAt": "2026-05-29T17:20:00"
  }
}
```

## 7. 平台配置模块

### 7.1 获取平台配置列表

```http
GET /api/configs/platforms
```

说明：前端根据该接口动态生成平台 Tab、字数限制、图片数量限制和标签校验规则。

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "platform": "xiaohongshu",
      "displayName": "小红书",
      "maxTitleLength": 20,
      "maxContentLength": 1000,
      "maxTags": 10,
      "supportsMarkdown": false,
      "image": {
        "maxCount": 9,
        "acceptedMimeTypes": ["image/jpeg", "image/png", "image/webp"]
      }
    }
  ]
}
```

### 7.2 获取单平台配置

```http
GET /api/configs/platforms/{platform}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "platform": "zhihu",
    "displayName": "知乎",
    "maxTitleLength": 80,
    "supportsMarkdown": true,
    "stylePrompt": "改写为专业、理性、结构化的问答或专栏风格。"
  }
}
```

## 8. Chrome 插件模块（真实发布可选）

### 8.1 插件注册

```http
POST /api/plugin/register
Content-Type: application/json
X-User-Token: ut_8f3c9a_demo
X-Trace-Id: trace_20260529_001
```

请求：

```json
{
  "sessionId": "plugin-session-001",
  "extensionVersion": "1.0.0",
  "browser": "Chrome",
  "clientSessionId": "web-session-001"
}
```

说明：插件注册必须绑定当前工作台的 `userToken`。后端以 `userToken + sessionId` 作为在线会话键，避免多个演示用户都传 `userId=1` 时互相覆盖。同一 `userToken` 下如果出现新的 `sessionId` 注册，后端可以将旧插件会话标记为 `OFFLINE`，并通过 WebSocket 推送 `PLUGIN_STATUS_CHANGED`。

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userToken": "ut_8f3c9a_demo",
    "sessionId": "plugin-session-001",
    "status": "ONLINE",
    "lastHeartbeatAt": "2026-05-29T17:10:20"
  }
}
```

### 8.2 插件心跳

```http
POST /api/plugin/heartbeat
Content-Type: application/json
X-User-Token: ut_8f3c9a_demo
```

请求：

```json
{
  "sessionId": "plugin-session-001"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userToken": "ut_8f3c9a_demo",
    "sessionId": "plugin-session-001",
    "status": "ONLINE",
    "lastHeartbeatAt": "2026-05-29T17:11:00"
  }
}
```

### 8.3 获取插件状态

```http
GET /api/plugin/status
X-User-Token: ut_8f3c9a_demo
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "userToken": "ut_8f3c9a_demo",
    "online": true,
    "sessionId": "plugin-session-001",
    "extensionVersion": "1.0.0",
    "lastHeartbeatAt": "2026-05-29T17:11:00"
  }
}
```

### 8.4 插件上报真实发布状态

```http
POST /api/plugin/publish-status
Content-Type: application/json
X-User-Token: ut_8f3c9a_demo
```

请求：

```json
{
  "sessionId": "plugin-session-001",
  "recordId": 9001,
  "platform": "xiaohongshu",
  "status": "SUSPENDED",
  "reason": "CAPTCHA_REQUIRED",
  "screenshotUrl": "http://localhost:8080/media/90001"
}
```

响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "recordId": 9001,
    "status": "SUSPENDED"
  }
}
```

## 9. WebSocket 事件

连接地址：

```text
ws://localhost:8080/ws/pipeline?userToken=ut_8f3c9a_demo&traceId=trace_20260529_001
```

用途：适配、发布、插件状态变化都通过该通道推送。后端应优先按照 `userToken` 定向推送，避免多用户演示时互相收到事件。前端仍必须在收到事件后校验 `data.taskId` 是否属于当前页面，非当前任务事件直接丢弃，并保留 HTTP 查询接口作为断线恢复兜底。

### 9.1 平台适配完成

```json
{
  "event": "PLATFORM_ADAPT_COMPLETED",
  "timestamp": 1779954061000,
  "data": {
    "userToken": "ut_8f3c9a_demo",
    "taskId": 1001,
    "recordId": 9001,
    "platform": "xiaohongshu",
    "status": "READY",
    "title": "别再手动复制了，内容分发这样做更快",
    "content": "亲爱的家人们！今天分享一个超绝的分发方案... ✨",
    "tags": ["搞定分发", "独立开发"],
    "media": [
      {
        "mediaId": 501,
        "publicUrl": "http://localhost:8080/media/501"
      }
    ],
    "styleExplanation": "已增加 Emoji 并转化为小红书口语化种草风。"
  }
}
```

### 9.2 平台适配降级

```json
{
  "event": "PLATFORM_ADAPT_DEGRADED",
  "timestamp": 1779954061000,
  "data": {
    "userToken": "ut_8f3c9a_demo",
    "taskId": 1001,
    "platform": "zhihu",
    "reason": "LLM API Key 缺失或请求超时，已自动降级为模板生成。"
  }
}
```

### 9.3 发布状态变化

```json
{
  "event": "PUBLISH_STATUS_CHANGED",
  "timestamp": 1779954061000,
  "data": {
    "userToken": "ut_8f3c9a_demo",
    "taskId": 1001,
    "recordId": 9001,
    "platform": "xiaohongshu",
    "status": "SUCCESS",
    "publishUrl": "http://localhost:8080/mock/xiaohongshu/9001"
  }
}
```

### 9.4 任务失败

```json
{
  "event": "TASK_FAILED",
  "timestamp": 1779954061000,
  "data": {
    "userToken": "ut_8f3c9a_demo",
    "taskId": 1001,
    "platform": "wechat",
    "status": "FAILED",
    "reason": "平台内容为空或图片资源不可访问。"
  }
}
```

### 9.5 插件状态变化

```json
{
  "event": "PLUGIN_STATUS_CHANGED",
  "timestamp": 1779954061000,
  "data": {
    "userToken": "ut_8f3c9a_demo",
    "sessionId": "plugin-session-001",
    "status": "ONLINE",
    "lastHeartbeatAt": "2026-05-29T17:11:00"
  }
}
```

## 10. 推荐前端调用流程

### 10.1 MVP 模拟发布流程

```text
POST /api/session/init
  -> WebSocket 连接 /ws/pipeline?userToken=...&traceId=...
  -> POST /api/tasks
  -> POST /api/tasks/{taskId}/media
  -> PUT /api/tasks/{taskId}/normalized
  -> POST /api/tasks/{taskId}/adapt
  -> 立即拿到 ADAPTING 占位 recordId 列表
  -> WebSocket 接收 PLATFORM_ADAPT_COMPLETED
  -> GET /api/tasks/{taskId}/records 兜底恢复
  -> PUT /api/records/{recordId} 保存人工编辑
  -> POST /api/tasks/{taskId}/publish mode=mock
  -> GET /api/mock/{platform}/{recordId}
  -> 前端路由 /#/mock/{platform}/{recordId} 渲染拟态页面
```

### 10.2 真实发布扩展流程

```text
POST /api/session/init
  -> 插件使用同一 X-User-Token 调用 POST /api/plugin/register
  -> 插件使用同一 X-User-Token 调用 POST /api/plugin/heartbeat
  -> 工作台使用同一 X-User-Token 调用 POST /api/tasks/{taskId}/publish mode=real
  -> 插件 fetch(publicUrl) 下载图片
  -> 插件填表上传
  -> POST /api/plugin/publish-status
  -> WebSocket 推送 PUBLISH_STATUS_CHANGED 或 SUSPENDED
```

## 11. 实现注意事项

1. `POST /api/tasks/{taskId}/adapt` 必须异步返回，避免 LLM 请求阻塞 HTTP。
2. `POST /api/tasks/{taskId}/adapt` 返回前必须先创建 `ADAPTING` 占位发布记录，并返回 `recordId` 列表。
3. 前端不能依赖 WebSocket 作为唯一数据来源，必须能通过任务和记录查询接口恢复状态。
4. WebSocket 必须带 `userToken`，后端定向推送；前端仍需按 `taskId` 二次过滤。
5. 所有图片必须先进入 `media_resource`，正文和发布记录只保存 `mediaId/publicUrl` 引用。
6. 删除媒体时必须同时检查标准内容模型和发布记录引用。
7. `GET /media/{mediaId}` 不应使用 `/api` 前缀，方便浏览器、Mock 页面和插件直接访问。
8. `real` 发布模式必须先检查插件在线态，并校验工作台与插件是否使用同一 `userToken/sessionId`。
9. 若使用 Vue 前端路由渲染 Mock 页面，优先使用 `/#/mock/{platform}/{recordId}`，后端只提供 `/api/mock/...` 数据接口。
10. MVP 阶段建议只实现 `mock` 发布，插件接口可以先保留文档和空实现。
