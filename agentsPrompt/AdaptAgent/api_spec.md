# Platform Adaptation Module API Specification (平台适配模块接口文档)

## 1. 接口定义

### 1.1 异步触发平台适配
*   **接口路径**：`POST /api/tasks/{taskId}/adapt`
*   **请求示例**：
    ```json
    {
      "platforms": ["xiaohongshu", "zhihu", "wechat", "bilibili"],
      "forceRegenerate": false
    }
    ```
*   **响应示例 (同步返回 202 Accepted 占位凭证)**：
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
          }
        ],
        "message": "平台适配已开始，请监听 WebSocket。"
      }
    }
    ```

### 1.2 获取平台发布记录列表
*   **接口路径**：`GET /api/tasks/{taskId}/records`
*   **说明**：用于工作台初始化、离线刷新或 WebSocket 断线恢复。
*   **响应示例**：
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

### 1.3 获取单条发布记录详情
*   **接口路径**：`GET /api/records/{recordId}`
*   **响应示例**：
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

### 1.4 保存人工编辑后的平台内容
*   **接口路径**：`PUT /api/records/{recordId}`
*   **说明**：在执行发布动作前，用户允许在前端工作台微调适配生成的标题、正文、标签及图片，以此接口回写数据库。发布时以人工修改后的最终版本为准。
*   **请求示例**：
    ```json
    {
      "adaptedTitle": "人工微调后的标题",
      "adaptedContent": "人工微调后的爆款正文 🚀 #干货分享",
      "tags": ["新标签1", "新标签2"],
      "mediaIds": [501]
    }
    ```
*   **响应示例**：
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

### 1.5 跳过某个平台
*   **接口路径**：`POST /api/records/{recordId}/skip`
*   **说明**：创作者对部分平台不进行内容投递时，显式标记跳过。
*   **响应示例**：
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
