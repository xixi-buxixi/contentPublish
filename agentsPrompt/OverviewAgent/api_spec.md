# Overview Module API Specification (总览模块接口文档)

## 1. 基础地址与数据格式
*   **API 基础路径**：`http://localhost:8080/api`
*   **默认 Content-Type**：`application/json`
*   **统一返回响应体**：
    ```json
    {
      "code": 200,    // 业务状态码: 200/202/400/404/409/500
      "message": "success",
      "data": {}
    }
    ```

## 2. 接口定义

### 2.1 会话初始化 (Session Init)
*   **接口路径**：`POST /api/session/init`
*   **说明**：工作台打开时首次调用，生成会话并返回身份凭证。后续 API 请求建议在 Header 中携带 `X-User-Token` 和 `X-Trace-Id`。
*   **请求参数**：
    ```json
    {
      "clientType": "web",      // 客户端类型
      "nickname": "demo-user"   // 昵称/备注
    }
    ```
*   **响应示例**：
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

### 2.2 WebSocket 长连接协议
*   **接口路径**：`ws://localhost:8080/ws/pipeline?userToken={userToken}&traceId={traceId}`
*   **说明**：工作台或插件连接时携带 Token 标识身份，后端定向推送该 Token 对应的所有任务和状态事件。
*   **全局推送事件格式**：
    所有事件帧均使用以下统一 JSON 包装结构：
    ```json
    {
      "event": "EVENT_NAME",      // 事件名称
      "timestamp": 1779954061000,  // 毫秒时间戳
      "data": {
        "userToken": "ut_8f3c9a_demo",
        // 具体事件负载 payload
      }
    }
    ```
