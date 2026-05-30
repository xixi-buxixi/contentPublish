package com.example.pulsedistro.controller;

import com.example.pulsedistro.dto.common.ApiResponse;
import com.example.pulsedistro.dto.publish.MockPageDataResponse;
import com.example.pulsedistro.service.MockPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MockPageController {

    private final MockPageService mockPageService;

    public MockPageController(MockPageService mockPageService) {
        this.mockPageService = mockPageService;
    }

    @GetMapping("/api/mock/{platform}/{recordId}")
    public ApiResponse<MockPageDataResponse> getMockData(
            @PathVariable String platform,
            @PathVariable String recordId
    ) {
        return ApiResponse.success(mockPageService.getMockData(platform, recordId));
    }

    @GetMapping(value = "/mock/{platform}/{recordId}", produces = "text/html;charset=UTF-8")
    public String getMockPage(@PathVariable String platform, @PathVariable String recordId) {
        MockPageDataResponse data = mockPageService.getMockData(platform, recordId);
        String normalizedPlatform = platform == null ? "" : platform.toLowerCase(java.util.Locale.ROOT);
        String mockBody = switch (normalizedPlatform) {
            case "xiaohongshu" -> renderXiaohongshu(data);
            case "zhihu" -> renderZhihu(data);
            case "wechat" -> renderWechat(data);
            case "bilibili" -> renderBilibili(data);
            default -> renderGeneric(data);
        };
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <link rel="preconnect" href="https://fonts.googleapis.com">
                  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Outfit:wght@600;700;800&display=swap" rel="stylesheet">
                  <script src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"></script>
                  <title>%s - Pulse Distro Mock</title>
                  <style>
                    :root { color-scheme: light; }
                    * { box-sizing: border-box; }
                    body {
                      min-height: 100vh;
                      margin: 0;
                      padding: clamp(1rem, 4vw, 3rem);
                      font-family: Inter, system-ui, sans-serif;
                      background:
                        radial-gradient(circle at top left, rgba(20, 184, 166, .18), transparent 30rem),
                        linear-gradient(135deg, #f8fafc, #eef2ff 52%%, #fdf2f8);
                      color: #111827;
                    }
                    .glass-panel {
                      inline-size: min(100%%, 56rem);
                      margin-inline: auto;
                      padding: clamp(1rem, 3vw, 2rem);
                      border: .0625rem solid rgba(255, 255, 255, .68);
                      border-radius: 1rem;
                      background: rgba(255, 255, 255, .72);
                      box-shadow: 0 1.5rem 4rem rgba(15, 23, 42, .14);
                      backdrop-filter: blur(1.125rem);
                    }
                    .glass-header {
                      display: flex;
                      align-items: center;
                      justify-content: space-between;
                      gap: 1rem;
                      padding-block-end: 1rem;
                      border-block-end: .0625rem solid rgba(15, 23, 42, .08);
                    }
                    .platform {
                      color: #64748b;
                      text-transform: uppercase;
                      font-size: .75rem;
                      letter-spacing: .12em;
                    }
                    .tag-list {
                      display: flex;
                      flex-wrap: wrap;
                      gap: .5rem;
                      margin-block-start: 1rem;
                    }
                    .tag-list span {
                      padding: .35rem .7rem;
                      border-radius: 999rem;
                      background: rgba(15, 23, 42, .06);
                      font-size: .8125rem;
                      color: #475569;
                    }
                    h1 {
                      margin: 1rem 0 1.25rem;
                      font-family: Outfit, Inter, system-ui, sans-serif;
                      font-size: clamp(1.5rem, 4vw, 2.5rem);
                      line-height: 1.12;
                    }
                    article { white-space: pre-wrap; line-height: 1.8; }
                    .xiaohongshu-mock {
                      inline-size: min(100%%, 28rem);
                      margin-inline: auto;
                      border-radius: 1.5rem;
                      background: linear-gradient(180deg, #fff7fb, #ffffff);
                      box-shadow: inset 0 0 0 .0625rem rgba(244, 114, 182, .28), 0 1.5rem 4rem rgba(190, 24, 93, .16);
                      overflow: hidden;
                    }
                    .xhs-feed { padding: 1rem; }
                    .xhs-cover {
                      aspect-ratio: 4 / 5;
                      border-radius: 1rem;
                      background: linear-gradient(135deg, #f9a8d4, #99f6e4);
                      display: grid;
                      place-items: center;
                      color: #831843;
                      font-family: Outfit, Inter, system-ui, sans-serif;
                      font-size: clamp(1.5rem, 8vw, 2.8rem);
                      text-align: center;
                      padding: 1rem;
                    }
                    .zhihu-mock {
                      display: grid;
                      gap: 1.25rem;
                      background: #fff;
                      border-radius: .75rem;
                      padding: clamp(1rem, 3vw, 2rem);
                      box-shadow: 0 1rem 3rem rgba(30, 64, 175, .10);
                    }
                    .zhihu-meta { color: #64748b; font-size: .9rem; }
                    .wechat-mock {
                      inline-size: min(100%%, 42rem);
                      margin-inline: auto;
                      background: #fff;
                      border-radius: 1.25rem;
                      padding: clamp(1.25rem, 4vw, 2.25rem);
                      box-shadow: 0 1rem 3rem rgba(22, 101, 52, .10);
                    }
                    .wechat-meta { color: #94a3b8; font-size: .875rem; }
                    .bilibili-mock {
                      background: #fff;
                      border-radius: 1rem;
                      padding: clamp(1rem, 3vw, 2rem);
                      border: .0625rem solid rgba(14, 165, 233, .18);
                      box-shadow: 0 1rem 3rem rgba(2, 132, 199, .12);
                    }
                    .bilibili-badge { color: #0ea5e9; font-weight: 700; }
                  </style>
                </head>
                <body>
                  <main class="glass-panel">
                    %s
                  </main>
                </body>
                </html>
                """.formatted(escape(data.title()), mockBody);
    }

    private String renderXiaohongshu(MockPageDataResponse data) {
        return """
                <section class="xiaohongshu-mock">
                  <div class="xhs-cover">%s</div>
                  <div class="xhs-feed">
                    <div class="platform">Xiaohongshu</div>
                    <h1>%s</h1>
                    <article>%s</article>
                    %s
                  </div>
                </section>
                """.formatted(escape(data.title()), escape(data.title()), htmlContent(data.content()), tags(data.tags(), "#"));
    }

    private String renderZhihu(MockPageDataResponse data) {
        return """
                <section class="zhihu-mock">
                  <div class="platform">Zhihu Answer</div>
                  <h1>%s</h1>
                  <div class="zhihu-meta">Pulse Distro · 模拟回答</div>
                  <article>%s</article>
                  %s
                </section>
                """.formatted(escape(data.title()), htmlContent(data.content()), tags(data.tags(), ""));
    }

    private String renderWechat(MockPageDataResponse data) {
        return """
                <section class="wechat-mock">
                  <div class="platform">WeChat Official Account</div>
                  <h1>%s</h1>
                  <div class="wechat-meta">Pulse Distro · 公众号预览</div>
                  <article>%s</article>
                  %s
                </section>
                """.formatted(escape(data.title()), htmlContent(data.content()), tags(data.tags(), ""));
    }

    private String renderBilibili(MockPageDataResponse data) {
        return """
                <section class="bilibili-mock">
                  <div class="glass-header">
                    <div class="bilibili-badge">Bilibili Dynamic</div>
                    <div class="platform">mock</div>
                  </div>
                  <h1>%s</h1>
                  <article>%s</article>
                  %s
                </section>
                """.formatted(escape(data.title()), htmlContent(data.content()), tags(data.tags(), "#"));
    }

    private String renderGeneric(MockPageDataResponse data) {
        return """
                <section>
                  <div class="glass-header">
                    <div class="platform">%s</div>
                  </div>
                  <h1>%s</h1>
                  <article>%s</article>
                  %s
                </section>
                """.formatted(escape(data.platform()), escape(data.title()), htmlContent(data.content()), tags(data.tags(), ""));
    }

    private String tags(java.util.List<String> tags, String prefix) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return tags.stream()
                .map(tag -> "<span>" + escape(prefix + tag) + "</span>")
                .collect(java.util.stream.Collectors.joining("", "<div class=\"tag-list\">", "</div>"));
    }

    private String htmlContent(String text) {
        return escape(text).replace("\n", "<br />");
    }

    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
