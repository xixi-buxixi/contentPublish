# Pulse Distro Agent Overview

本文件是 Codex 进入项目时优先读取的总览导航。它面向项目构建和模块协作，不替代用户当前指令。

## 1. 项目描述

Pulse Distro 是一个多平台内容自动适配与分发工具。MVP 目标是在单体 Java 应用中完成：

- 输入一份原始内容。
- 生成适合小红书、知乎、微信公众号、B 站的发布版本。
- 支持多平台预览和人工编辑。
- 通过一键 `mock` 模拟发布生成发布记录和拟态页面。
- 为后续 `real` 浏览器插件发布保留接口，但不让真实发布阻塞 MVP。

当前仓库主要包含原型页面、需求/接口资料、Codex agent 上下文和 hook。实际 Java 源码可按本文件导航逐步生成。

## 2. 构建主线

推荐实现形态：

- Java 21 + Spring Boot 3.3.x。
- H2 文件数据库。
- 本地媒体存储：`data/media/{taskId}/`。
- Vue 3 或静态前端打包进 Spring Boot `static`。
- WebSocket 用于适配、发布、插件状态的增量推送。
- LangChain4j 用于智能改写；缺少模型配置时必须降级到模板适配。

MVP 优先顺序：

1. 内容任务与标准内容模型。
2. 媒体上传、访问和引用校验。
3. 平台配置加载。
4. 异步内容适配和模板降级。
5. 一键 Mock 发布和拟态页。
6. 会话隔离与 WebSocket 推送。
7. 插件在线态和真实发布扩展。

## 3. 模块导航

| 模块 | 入口 | 主要职责 | 依赖关系 |
| --- | --- | --- | --- |
| OverviewAgent | `agentsPrompt/OverviewAgent/agent.md` | 会话初始化、WebSocket、全局事件路由 | 依赖各业务模块产生事件 |
| TaskAgent | `agentsPrompt/TaskAgent/agent.md` | 内容任务、标准内容模型、Markdown 解析 | 被 Adapt/Media/Publish 使用 |
| MediaAgent | `agentsPrompt/MediaAgent/agent.md` | 媒体上传、本地存储、二进制访问、引用删除 | 被 Task/Adapt/Publish 引用 |
| ConfigAgent | `agentsPrompt/ConfigAgent/agent.md` | 平台规则配置、配置加载、规则查询 | 被 Adapt/Publish/前端使用 |
| AdaptAgent | `agentsPrompt/AdaptAgent/agent.md` | 异步平台适配、LLM/模板降级、适配记录 | 依赖 Task/Media/Config，通知 Overview |
| PublishAgent | `agentsPrompt/PublishAgent/agent.md` | 一键发布、MockPublisher、拟态页面 | 依赖 Adapt/Config/Media，通知 Overview |
| PluginAgent | `agentsPrompt/PluginAgent/agent.md` | 浏览器插件注册、心跳、真实发布回调 | 依赖 Overview 会话，服务 Publish real 模式 |

## 4. 总览 Agent 的注入职责

总览 agent 不要求子 agent 直接读取需求文档、接口文档、prompt 或 hook。总览 agent 负责：

1. 根据任务范围选择对应模块。
2. 从总览资料中提取必要的接口、状态、数据模型和约束。
3. 把提取后的最小上下文写入或同步到对应模块 `agent.md`。
4. 触发对应模块 agent 时，让其读取本文件和自己的模块 `agent.md` 即可。

跨模块信息必须通过模块 `agent.md` 中的“关联模块”和“接口边界”呈现，避免子 agent 为了找上下文反复读取大文档。

## 5. Agent 不需要读取的文件

普通模块 agent 默认不需要读取以下文件，除非用户明确要求做文档维护或总览 agent 正在提取上下文：

- `Pulse-Distro-完整架构方案.md`
- `Pulse-Distro-Java轻量实践方案.md`
- `接口文档/`
- `agentsPrompt/**/requirements.md`
- `agentsPrompt/**/api_spec.md`
- `agentsPrompt/**/prompt.md`
- `agentsPrompt/CODEX_AGENT_CONTRACT.md`
- `hooks/`
- `tests/`
- `task_plan.md`
- `findings.md`
- `progress.md`

这些文件是总览 agent、hook 测试或人工维护资料。业务实现 agent 应主要读取：

- 根目录 `agent.md`
- 当前模块 `agentsPrompt/<ModuleAgent>/agent.md`
- 实际源码和测试文件

## 6. 全局约定

- API 边界中的平台标识使用小写：`xiaohongshu`、`zhihu`、`wechat`、`bilibili`。
- API 边界中的发布模式使用小写：`mock`、`real`。
- 状态使用大写：`PENDING`、`ADAPTING`、`READY`、`PUBLISHING`、`SUCCESS`、`VERIFIED_SUCCESS`、`WARN`、`SUSPENDED`、`FAILED`、`SKIPPED`。
- Java enum 可内部大写，但 DTO/Controller 边界必须转换。
- `platform_publish_record.publish_mode` 在适配占位阶段允许为空；发布阶段再写入 `mock` 或 `real`。
- WebSocket 推送必须按 `userToken` 定向发送，前端仍需按 `taskId` 二次过滤。
- 代码出错时直接修复根因并验证，不要只做装饰性补丁或绕开问题。

## 7. Agent.md 更新节奏

每个 agent hook 按模块记录轮次。每 3 轮后必须更新：

- 根目录 `agent.md`
- 当前模块 `agentsPrompt/<ModuleAgent>/agent.md`

更新内容包括：

- 新增或变化的模块职责。
- 新增源码路径和测试路径。
- 新增跨模块依赖或接口边界。
- 已发现但尚未解决的风险。

旧的 `agent.md` 不能无限复用。hook 会检查文件修改时间，确认这些文档在上一轮记录之后被刷新。
