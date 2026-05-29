# Publishing & Mocking Module Requirements (发布与 Mock 模块需求)

## 1. 发布器抽象设计 (Publisher Interface)
```java
public interface Publisher {
    PublishMode mode(); // MOCK 或 REAL
    PublishResult publish(PublishContext context, AdaptedContent content, PlatformRule rule);
}
```
*   **MockPublisher 行为**：
    1.  检测各平台要求的字段是否存在（标题、正文、标签）。
    2.  检测 `media` 中的所有 URL 在外部网络可正常访问。
    3.  立刻将 `platform_publish_record` 状态更新为 `SUCCESS`。
    4.  写入 `published_at` 为当前时间，生成 `/mock/{platform}/{recordId}` 模拟页面地址。
    5.  通过 WebSocket 推送最新的发布状态 `PUBLISH_STATUS_CHANGED`。

## 2. 状态机流转细则
*   单平台适配完成后的初始状态为 `READY`。
*   一键发布接口触发后，状态机转移为 `PUBLISHING`。
*   根据发布器的执行结果，转移为 `SUCCESS`（成功）或 `FAILED`（失败，并在 `error_message` 中记录错误原因）。

## 3. 高度拟态预览详情页规范
为保证出色的演示效果，各平台的 Mock 预览页应具备以下视觉特色（推荐使用现代高级 CSS 样式）：
*   **小红书 (xiaohongshu)**：
    *   **手机外壳**：整体文案和插图放在一个圆角、带阴影的手机容器内。
    *   **滑动切图**：多张图支持左右滑动查看。
    *   **排版细节**：带有标志性的红底白字小红书 UI 头部、底部的爱心/收藏交互、 Emoji 段落、多标签高亮（#话题）。
*   **知乎 (zhihu)**：
    *   **PC 专栏版式**：白色卡片底色，搭配经典的知乎蓝高亮。
    *   **排版细节**：标题、作者头像、赞同按钮、评论区骨架屏。
*   **微信公众号 (wechat)**：
    *   **移动长文**：长滚动页面，顶部显示公众号作者、发布时间。
    *   **排版细节**：字号较大，段落行间距留白大，包含微信专属绿色主色调的顶部/底部装饰线。
*   **B站 (bilibili)**：
    *   **动态卡片**：顶部为用户动态小卡片，支持图文列表，底部为点赞、投币、收藏、转发的四大经典小图标。
