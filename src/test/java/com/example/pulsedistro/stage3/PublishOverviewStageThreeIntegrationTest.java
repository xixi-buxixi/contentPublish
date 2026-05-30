package com.example.pulsedistro.stage3;

import com.example.pulsedistro.dto.adapt.AdaptRequest;
import com.example.pulsedistro.dto.adapt.AdaptStartResponse;
import com.example.pulsedistro.dto.publish.MockPageDataResponse;
import com.example.pulsedistro.dto.publish.PublishRequest;
import com.example.pulsedistro.dto.publish.PublishResponse;
import com.example.pulsedistro.dto.session.SessionInitRequest;
import com.example.pulsedistro.dto.session.SessionInitResponse;
import com.example.pulsedistro.dto.task.CreateTaskRequest;
import com.example.pulsedistro.dto.task.TaskSummaryResponse;
import com.example.pulsedistro.domain.MediaResource;
import com.example.pulsedistro.domain.PlatformPublishRecord;
import com.example.pulsedistro.event.PipelineEvent;
import com.example.pulsedistro.exception.BusinessException;
import com.example.pulsedistro.model.MediaRef;
import com.example.pulsedistro.repository.MediaResourceRepository;
import com.example.pulsedistro.repository.PlatformPublishRecordRepository;
import com.example.pulsedistro.service.AdaptService;
import com.example.pulsedistro.service.MediaService;
import com.example.pulsedistro.controller.MockPageController;
import com.example.pulsedistro.service.MockPageService;
import com.example.pulsedistro.service.PipelineEventPublisher;
import com.example.pulsedistro.service.PublishService;
import com.example.pulsedistro.service.SessionService;
import com.example.pulsedistro.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "LANGCHAIN4J_API_KEY=")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PublishOverviewStageThreeIntegrationTest {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private AdaptService adaptService;

    @Autowired
    private MediaService mediaService;

    @Autowired
    private PublishService publishService;

    @Autowired
    private MockPageService mockPageService;

    @Autowired
    private MockPageController mockPageController;

    @Autowired
    private PipelineEventPublisher eventPublisher;

    @Autowired
    private MediaResourceRepository mediaRepository;

    @Autowired
    private PlatformPublishRecordRepository recordRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @Value("${pulse.media.storage-root}")
    private String storageRoot;

    @Test
    void sessionInitGeneratesIsolatedUserTokenAndTraceId() {
        SessionInitResponse session = sessionService.init(new SessionInitRequest("web", "demo-user"));

        assertThat(session.userId()).isPositive();
        assertThat(session.userToken()).startsWith("ut_");
        assertThat(session.traceId()).startsWith("trace_");
    }

    @Test
    void emptySessionInitBodyStillSucceeds() throws Exception {
        mockMvc.perform(post("/api/session/init")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userToken").isNotEmpty());
    }

    @Test
    void mockPublishUpdatesReadyRecordsAndPublishesUserScopedEvents() {
        SessionInitResponse session = sessionService.init(new SessionInitRequest("web", "publisher"));
        TaskSummaryResponse task = taskService.createTask(new CreateTaskRequest(
                "Mock 发布任务",
                "MARKDOWN",
                "# 开场\n\n发布前保存编辑内容。\n\n- mock 先跑通"
        ));
        AdaptStartResponse started = adaptService.startAdaptation(task.taskId(), new AdaptRequest(
                List.of("xiaohongshu"),
                false
        ), session.userToken(), session.traceId());
        await().untilAsserted(() -> assertThat(adaptService.getRecord(started.records().getFirst().recordId()).status())
                .isEqualTo("READY"));

        PublishResponse published = publishService.publish(task.taskId(), new PublishRequest(
                "mock",
                List.of("xiaohongshu"),
                "web-session-001"
        ), session.userToken(), session.traceId());

        assertThat(published.mode()).isEqualTo("mock");
        assertThat(published.results()).hasSize(1);
        assertThat(published.results().getFirst().status()).isEqualTo("SUCCESS");
        assertThat(published.results().getFirst().publishUrl())
                .isEqualTo("http://localhost:8080/mock/xiaohongshu/" + started.records().getFirst().recordId());

        MockPageDataResponse mockData = mockPageService.getMockData("xiaohongshu", started.records().getFirst().recordId());
        assertThat(mockData.title()).isNotBlank();
        assertThat(mockData.content()).contains("发布前保存编辑内容");

        List<PipelineEvent> events = eventPublisher.recentEventsFor(session.userToken());
        assertThat(events)
                .extracting(PipelineEvent::event)
                .contains("PUBLISH_STATUS_CHANGED");
        assertThat(events.getLast().data()).containsEntry("userToken", session.userToken());
    }

    @Test
    void mockPublishRejectsMissingLocalMediaFiles() throws Exception {
        SessionInitResponse session = sessionService.init(new SessionInitRequest("web", "media-check"));
        TaskSummaryResponse task = taskService.createTask(new CreateTaskRequest(
                "媒体校验任务",
                "MARKDOWN",
                "正文"
        ));
        MockMultipartFile image = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                tinyPng()
        );
        String mediaId = mediaService.store(task.taskId(), image, "封面").mediaId();
        MediaResource media = mediaRepository.findById(mediaId).orElseThrow();
        Files.delete(Path.of(storageRoot).toAbsolutePath().normalize().resolve(media.getStorageKey()).normalize());

        PlatformPublishRecord record = new PlatformPublishRecord(task.taskId(), "xiaohongshu");
        record.markReady(
                "标题",
                "正文",
                "[]",
                objectMapper.writeValueAsString(List.of(new MediaRef(media.getId(), media.getPublicUrl(), "封面", 1, 1))),
                "ready"
        );
        recordRepository.save(record);

        assertThatThrownBy(() -> publishService.publish(task.taskId(), new PublishRequest(
                "mock",
                List.of("xiaohongshu"),
                "web-session-001"
        ), session.userToken(), session.traceId()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    @Test
    void mockPageHtmlUsesPlatformSpecificLayouts() {
        SessionInitResponse session = sessionService.init(new SessionInitRequest("web", "mock-page"));
        TaskSummaryResponse task = taskService.createTask(new CreateTaskRequest(
                "拟态页面任务",
                "MARKDOWN",
                "正文"
        ));
        AdaptStartResponse started = adaptService.startAdaptation(task.taskId(), new AdaptRequest(
                List.of("xiaohongshu", "zhihu", "wechat", "bilibili"),
                false
        ), session.userToken(), session.traceId());
        await().untilAsserted(() -> assertThat(adaptService.listRecords(task.taskId()))
                .extracting(record -> record.platform() + ":" + record.status())
                .containsExactlyInAnyOrder(
                        "xiaohongshu:READY",
                        "zhihu:READY",
                        "wechat:READY",
                        "bilibili:READY"
                ));
        publishService.publish(task.taskId(), new PublishRequest(
                "mock",
                List.of("xiaohongshu", "zhihu", "wechat", "bilibili"),
                "web-session-001"
        ), session.userToken(), session.traceId());

        String xhsHtml = mockPageController.getMockPage("xiaohongshu", started.records().get(0).recordId());
        String zhihuHtml = mockPageController.getMockPage("zhihu", started.records().get(1).recordId());
        String wechatHtml = mockPageController.getMockPage("wechat", started.records().get(2).recordId());
        String bilibiliHtml = mockPageController.getMockPage("bilibili", started.records().get(3).recordId());

        assertThat(xhsHtml).contains("xiaohongshu-mock").doesNotContain("xhs-shell");
        assertThat(zhihuHtml).contains("zhihu-mock").doesNotContain("zhihu-answer");
        assertThat(wechatHtml).contains("wechat-mock").doesNotContain("wechat-article");
        assertThat(bilibiliHtml).contains("bilibili-mock").doesNotContain("bilibili-dynamic");
        assertThat(List.of(xhsHtml, zhihuHtml, wechatHtml, bilibiliHtml)).doesNotHaveDuplicates();
    }

    @Test
    void realPublishFailsFastWhenPluginIsOffline() {
        SessionInitResponse session = sessionService.init(new SessionInitRequest("web", "real-mode"));
        TaskSummaryResponse task = taskService.createTask(new CreateTaskRequest(
                "真实发布任务",
                "MARKDOWN",
                "正文"
        ));

        assertThatThrownBy(() -> publishService.publish(task.taskId(), new PublishRequest(
                "real",
                List.of("zhihu"),
                "web-session-001"
        ), session.userToken(), session.traceId()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(400);
    }

    private byte[] tinyPng() {
        return java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII="
                        .getBytes(StandardCharsets.UTF_8)
        );
    }
}
