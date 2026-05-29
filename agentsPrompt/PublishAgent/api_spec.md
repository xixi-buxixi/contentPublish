# Publishing & Mocking Module API Specification (发布与 Mock 模块接口文档)

## 1. 接口定义

### 1.1 一键发布
*   **接口路径**：`POST /api/tasks/{taskId}/publish`
*   **说明**：触发模拟发布或真实发布。MVP 默认运行 `mode=mock`。
*   **请求示例**：
    ```json
    {
      "mode": "mock",                                         // 发布模式
      "platforms": ["xiaohongshu", "zhihu", "wechat"]         // 待发布的平台列表
    }
    ```
*   **响应示例**：
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

### 1.2 访问平台模拟发布效果页
*   **接口路径**：`GET /mock/{platform}/{recordId}`
*   **说明**：不带 `/api` 前缀。直接响应经过 Thymeleaf 模板解析或直接渲染的 HTML 网页，呈现与平台高度仿真的视觉布局。
*   **主要响应头**：
    ```http
    Content-Type: text/html;charset=UTF-8
    ```

### 1.3 获取模拟页面数据 (JSON API 兜底)
*   **接口路径**：`GET /api/mock/{platform}/{recordId}`
*   **说明**：如果前端使用 Vue 路由（如 `/#/mock/{platform}/{recordId}`）进行单页面渲染，可以通过调用该接口获取原始的发布文案及插图，以自行呈现拟态组件。
*   **响应示例**：
    ```json
    {
      "code": 200,
      "message": "success",
      "data": {
        "recordId": 9001,
        "platform": "xiaohongshu",
        "title": "修改后的小红书标题",
        "content": "修改后的爆款正文 🚀 #干货分享",
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
