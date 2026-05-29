# Media Resource Module API Specification (媒体资源模块接口文档)

## 1. 接口定义

### 1.1 上传媒体文件
*   **接口路径**：`POST /api/tasks/{taskId}/media`
*   **Content-Type**：`multipart/form-data`
*   **请求参数**：
    *   `file` (MultipartFile, 必须): 文件对象。
    *   `alt` (String, 可选): 图片说明。
*   **响应示例**：
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

### 1.2 获取任务媒体列表
*   **接口路径**：`GET /api/tasks/{taskId}/media`
*   **响应示例**：
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

### 1.3 访问媒体文件 (公共免签流)
*   **接口路径**：`GET /media/{mediaId}`
*   **注意**：此接口不带 `/api` 路径前缀。直接响应文件的 InputStream 字节流。
*   **主要响应头**：
    ```http
    Content-Type: image/png  (根据 mime_type 变化)
    ```

### 1.4 删除媒体文件 (带引用强校验)
*   **接口路径**：`DELETE /api/tasks/{taskId}/media/{mediaId}`
*   **安全删除响应 (未被引用)**：
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
*   **引用冲突响应 (已被正文块/发布记录引用)**：
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
