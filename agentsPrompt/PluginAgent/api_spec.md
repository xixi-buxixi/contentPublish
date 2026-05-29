# Chrome Extension Module API Specification (浏览器插件模块接口文档)

## 1. 接口定义

### 1.1 插件注册
*   **接口路径**：`POST /api/plugin/register`
*   **说明**：Chrome 插件启动或状态激活时调用，进行身份绑定。
*   **请求示例**：
    ```json
    {
      "sessionId": "plugin-session-001",
      "extensionVersion": "1.0.0",
      "browser": "Chrome"
    }
    ```
*   **响应示例**：
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

### 1.2 插件心跳
*   **接口路径**：`POST /api/plugin/heartbeat`
*   **请求示例**：
    ```json
    {
      "sessionId": "plugin-session-001"
    }
    ```
*   **响应示例**：
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

### 1.3 获取插件状态 (工作台查询)
*   **接口路径**：`GET /api/plugin/status`
*   **说明**：工作台查询当前登录用户对应的插件是否在线。
*   **响应示例**：
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

### 1.4 插件上报真实发布状态
*   **接口路径**：`POST /api/plugin/publish-status`
*   **说明**：真实发布模式下，插件将填表或发布中途的状态（如遇到验证码、异常或成功）回传给后端，触发工作台状态刷新。
*   **请求示例**：
    ```json
    {
      "sessionId": "plugin-session-001",
      "recordId": 9001,
      "platform": "xiaohongshu",
      "status": "SUSPENDED",                     // 挂起状态
      "reason": "CAPTCHA_REQUIRED",              // 遇到验证码
      "screenshotUrl": "http://localhost:8080/media/90001" // 截屏图片的 publicUrl 引用
    }
    ```
*   **响应示例**：
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
