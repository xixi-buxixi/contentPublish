# Overview Module Requirements (总览模块需求)

## 1. 架构目标
*   **轻量化单体**：系统必须运行在单个 JVM 进程中，通过 Spring Boot 3.3.x 提供 HTTP 和 WebSocket 服务。
*   **零外部依赖**：默认不依赖独立运行的 Redis、MySQL、Python 服务。数据持久化依赖 H2 文件模式，媒体图片保存在本地磁盘中。
*   **前端交付**：Vue 3 静态前端打包后放入 Spring Boot 的 `static` 目录中，最终构建成一个可执行 Jar 包。

## 2. 会话初始化与多用户隔离
*   虽然系统不做完整的注册和登录验证，但必须给每个工作台分配唯一的会话凭证，支持多浏览器标签或多人在同一服务实例上演示。
*   **会话三元组**：包括 `userId` (Long), `userToken` (String), `traceId` (String)。
*   **绑定机制**：客户端在建立 WebSocket 连接或注册 Chrome 插件时必须携带 `userToken`。后端以此实现工作台（Web Console）与执行端（Browser Plugin）的配对关联。

## 3. WebSocket 数据推送流水线
*   **长连接地址**：`ws://localhost:8080/ws/pipeline?userToken={userToken}&traceId={traceId}`。
*   **定向推送**：后端在向外发送 `PLATFORM_ADAPT_COMPLETED` (适配完成)、`PLATFORM_ADAPT_DEGRADED` (适配降级)、`PUBLISH_STATUS_CHANGED` (发布状态变更)、`TASK_FAILED` (任务失败) 等事件时，必须根据 `userToken` 定向发送到对应的 WebSocket Session 中。
*   **并发要求**：后端在推送消息时需要处理并发写入同一个 WebSocket Session 的竞态问题。
