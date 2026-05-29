# Content Task Module API Specification (内容任务模块接口文档)

## 1. 接口定义

### 1.1 创建内容任务
*   **接口路径**：`POST /api/tasks`
*   **请求示例**：
    ```json
    {
      "title": "如何高效进行多平台内容分发",
      "sourceType": "MARKDOWN",
      "rawContent": "# 核心观点\n别再手动复制了..."
    }
    ```
*   **响应示例**：
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

### 1.2 获取任务详情
*   **接口路径**：`GET /api/tasks/{taskId}`
*   **响应示例**：
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

### 1.3 获取标准内容模型
*   **接口路径**：`GET /api/tasks/{taskId}/normalized`
*   **说明**：用于工作台前端编辑器加载解析后的正文块和图片引用。
*   **响应示例**：
    ```json
    {
      "code": 200,
      "message": "success",
      "data": {
        "title": "如何高效进行多平台内容分发",
        "summary": "别再手动复制了，内容分发这样做更快。",
        "blocks": [
          {
            "type": "paragraph",
            "level": null,
            "text": "这是正文第一段。",
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

### 1.4 更新标准内容模型
*   **接口路径**：`PUT /api/tasks/{taskId}/normalized`
*   **说明**：前端修改正文块或插入图片后将最新结构模型回写，后端必须进行媒体引用的安全校验。
*   **请求示例**：
    ```json
    {
      "title": "更新后的标题",
      "summary": "更新后的摘要",
      "blocks": [
        {
          "type": "paragraph",
          "text": "修改后的正文段落。"
        },
        {
          "type": "image",
          "media": {
            "mediaId": 501,
            "alt": "新的插图说明"
          }
        }
      ]
    }
    ```
*   **响应示例**：
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
