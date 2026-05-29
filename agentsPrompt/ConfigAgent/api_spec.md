# Platform Config Module API Specification (平台配置模块接口文档)

## 1. 接口定义

### 1.1 获取平台配置列表
*   **接口路径**：`GET /api/configs/platforms`
*   **说明**：拉取所有已启用的平台规则，用于工作台前置验证以及 UI 字数限制展示。
*   **响应示例**：
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

### 1.2 获取单平台规则配置
*   **接口路径**：`GET /api/configs/platforms/{platform}`
*   **响应示例**：
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
