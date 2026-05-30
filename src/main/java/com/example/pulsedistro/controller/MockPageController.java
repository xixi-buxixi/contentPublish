package com.example.pulsedistro.controller;

import com.example.pulsedistro.dto.common.ApiResponse;
import com.example.pulsedistro.dto.publish.MockPageDataResponse;
import com.example.pulsedistro.model.MediaRef;
import com.example.pulsedistro.service.MockPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.stream.Collectors;

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
        String normalizedPlatform = platform == null ? "" : platform.toLowerCase(Locale.ROOT);
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
                    article { white-space: pre-wrap; line-height: 1.8; }
                    .xiaohongshu-mock { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
                    .zhihu-mock { font-family: -apple-system,BlinkMacSystemFont,Helvetica Neue,PingFang SC,Microsoft YaHei,sans-serif; }
                    .wechat-mock { font-family: -apple-system,BlinkMacSystemFont,Helvetica Neue,sans-serif; }
                    .bilibili-mock { font-family: -apple-system,BlinkMacSystemFont,Helvetica Neue,Arial,sans-serif; }
                    .xhs-cover {
                      background: linear-gradient(135deg, #f9a8d4, #99f6e4);
                      display: grid;
                      place-items: center;
                      color: #831843;
                      font-family: Outfit, Inter, system-ui, sans-serif;
                      font-size: clamp(1.5rem, 8vw, 2.8rem);
                      text-align: center;
                      padding: 1rem;
                    }
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
                <section class="xiaohongshu-mock w-full max-w-md bg-white border-[0.75rem] border-slate-900 rounded-[2.25rem] shadow-2xl overflow-hidden flex flex-col text-slate-800 text-xs mx-auto">
                  <div class="h-6 bg-white px-5 flex justify-between items-center text-[10px] font-semibold text-slate-700">
                    <span>20:46</span>
                    <span class="flex gap-1">信号 电量</span>
                  </div>
                  <div class="h-11 border-b border-slate-100 flex items-center justify-between px-4">
                    <span class="text-sm">‹</span>
                    <div class="flex items-center gap-2">
                      <div class="w-7 h-7 rounded-full bg-gradient-to-tr from-rose-400 to-amber-400"></div>
                      <span class="font-semibold text-slate-800">极客独立创作者</span>
                    </div>
                    <span class="border border-rose-500 text-rose-500 text-[10px] px-2.5 py-0.5 rounded-full font-semibold">关注</span>
                  </div>
                  <div class="relative w-full aspect-square bg-slate-50 flex items-center justify-center">
                    %s
                    <div class="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1">
                      <div class="w-1.5 h-1.5 rounded-full bg-rose-500"></div>
                      <div class="w-1.5 h-1.5 rounded-full bg-white/60"></div>
                    </div>
                  </div>
                  <div class="p-4 flex-grow overflow-y-auto">
                    <div class="text-sm font-bold text-slate-900 mb-2 leading-snug">%s</div>
                    <article class="leading-relaxed whitespace-pre-wrap text-slate-700">%s</article>
                    %s
                    <div class="text-[9px] text-slate-400 mt-4">编辑于 今天 20:46 · IP 属地浙江</div>
                  </div>
                  <div class="h-12 border-t border-slate-100 flex items-center justify-between px-4 bg-white text-slate-500">
                    <span>说点什么...</span>
                    <div class="flex gap-4">
                      <div class="flex items-center gap-1">喜欢 <span class="text-[10px]">2.8k</span></div>
                      <div class="flex items-center gap-1">收藏 <span class="text-[10px]">1.4k</span></div>
                      <div class="flex items-center gap-1">评论 <span class="text-[10px]">582</span></div>
                    </div>
                  </div>
                </section>
                """.formatted(coverImage(data), escape(data.title()), htmlContent(data.content()), tags(data.tags(), "#"));
    }

    private String renderZhihu(MockPageDataResponse data) {
        return """
                <section class="zhihu-mock w-full max-w-3xl bg-white border border-slate-200 rounded-lg shadow-sm p-6 flex flex-col gap-4 text-slate-800 text-sm mx-auto">
                  <div class="text-xl font-semibold leading-snug text-slate-900">%s</div>
                  <div class="flex items-center gap-2.5">
                    <div class="w-9 h-9 rounded bg-gradient-to-tr from-blue-500 to-indigo-500"></div>
                    <div class="flex flex-col">
                      <span class="font-bold text-slate-700 text-xs">知乎科技创作者</span>
                      <span class="text-[10px] text-slate-400">全栈工程师 / 自媒体系统架构师</span>
                    </div>
                  </div>
                  <article class="leading-relaxed whitespace-pre-wrap text-slate-800">%s</article>
                  %s
                  <div class="flex items-center justify-between pt-4 border-t border-slate-100">
                    <div class="flex gap-2">
                      <button class="bg-blue-50 text-blue-600 border-none px-3.5 py-1.5 rounded-sm font-medium text-xs flex items-center gap-1.5">▲ 赞同 4.2k</button>
                      <button class="bg-blue-50 text-blue-600 border-none px-2.5 py-1.5 rounded-sm text-xs">▼</button>
                    </div>
                    <span class="text-xs text-slate-400">128 条评论 · 分享 · 收藏</span>
                  </div>
                </section>
                """.formatted(escape(data.title()), htmlContent(data.content()), tags(data.tags(), ""));
    }

    private String renderWechat(MockPageDataResponse data) {
        return """
                <section class="wechat-mock w-full max-w-md bg-white border-[0.75rem] border-slate-900 rounded-[2.25rem] shadow-2xl overflow-hidden flex flex-col text-slate-800 text-xs mx-auto">
                  <div class="h-6 bg-white px-5 flex justify-between items-center text-[10px] font-semibold text-slate-700">
                    <span>20:46</span>
                    <span class="flex gap-1">信号 电量</span>
                  </div>
                  <div class="h-11 border-b border-slate-100 flex items-center gap-4 px-4 bg-white">
                    <span class="text-sm">‹</span>
                    <span class="font-medium text-slate-800 text-sm">公众号阅读</span>
                  </div>
                  <div class="p-5 overflow-y-auto flex-grow">
                    <div class="text-lg font-bold leading-snug text-slate-900 mb-3">%s</div>
                    <div class="flex gap-2 text-slate-400 mb-4 text-[10px]">
                      <span>2026-05-29</span>
                      <span class="text-blue-500 font-semibold">PulseDistro官微</span>
                      <span class="text-slate-300">极客学术</span>
                    </div>
                    <div class="bg-slate-50 border border-slate-200/50 p-3.5 text-[11px] text-slate-500 rounded-lg mb-5 leading-relaxed">
                      <strong>摘要：</strong>%s
                    </div>
                    <article class="leading-relaxed text-slate-700">%s</article>
                    %s
                  </div>
                </section>
                """.formatted(escape(data.title()), escape(data.summary()), htmlContent(data.content()), tags(data.tags(), ""));
    }

    private String renderBilibili(MockPageDataResponse data) {
        return """
                <section class="bilibili-mock w-full max-w-3xl bg-white border border-slate-200 rounded-lg shadow-sm p-6 flex flex-col gap-4 text-slate-800 text-sm mx-auto">
                  <div class="text-slate-400 text-xs flex gap-1 items-center">
                    <span>专栏区</span> / <span>科技数码</span>
                  </div>
                  <div class="text-lg font-bold text-slate-900 leading-snug">%s</div>
                  <div class="flex flex-wrap gap-4 text-[10px] text-slate-400 border-b border-slate-100 pb-3">
                    <span>作者: 科技Up主小助手</span>
                    <span>时间: 2026-05-29 20:46</span>
                    <span>阅读: 12.8k</span>
                    <span>评论: 452</span>
                  </div>
                  <article class="leading-relaxed text-slate-800 text-[13.5px]">%s</article>
                  %s
                  <div class="flex flex-wrap gap-6 pt-4 border-t border-slate-100 text-slate-400 text-xs mt-3">
                    <div class="flex items-center gap-1">点赞 <span>(2.4k)</span></div>
                    <div class="flex items-center gap-1">投币 <span>(1.8k)</span></div>
                    <div class="flex items-center gap-1">收藏 <span>(1.2k)</span></div>
                    <div class="flex items-center gap-1">分享 <span>(320)</span></div>
                  </div>
                </section>
                """.formatted(escape(data.title()), htmlContent(data.content()), tags(data.tags(), "#"));
    }

    private String renderGeneric(MockPageDataResponse data) {
        return """
                <section>
                  <div class="text-xs uppercase tracking-[0.12em] text-slate-500">%s</div>
                  <h1 class="font-title text-3xl font-bold">%s</h1>
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
                .collect(Collectors.joining("", "<div class=\"tag-list\">", "</div>"));
    }

    private String htmlContent(String text) {
        return escape(text).replace("\n", "<br />");
    }

    private String coverImage(MockPageDataResponse data) {
        MediaRef firstMedia = data.media() == null || data.media().isEmpty() ? null : data.media().getFirst();
        if (firstMedia != null && firstMedia.publicUrl() != null) {
            return "<img src=\"" + escape(firstMedia.publicUrl()) + "\" alt=\""
                    + escape(firstMedia.alt()) + "\" class=\"w-full h-full object-cover\">";
        }
        return "<div class=\"xhs-cover w-full h-full\">" + escape(data.title()) + "</div>";
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
