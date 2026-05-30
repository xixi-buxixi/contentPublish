package com.example.pulsedistro.stage2;

import com.example.pulsedistro.dto.adapt.AdaptRequest;
import com.example.pulsedistro.dto.adapt.AdaptStartResponse;
import com.example.pulsedistro.dto.adapt.RecordDetailResponse;
import com.example.pulsedistro.dto.adapt.RecordSummaryResponse;
import com.example.pulsedistro.dto.task.CreateTaskRequest;
import com.example.pulsedistro.dto.task.TaskSummaryResponse;
import com.example.pulsedistro.event.PipelineEvent;
import com.example.pulsedistro.model.PlatformRule;
import com.example.pulsedistro.repository.ContentTaskRepository;
import com.example.pulsedistro.service.AdaptationModelClient;
import com.example.pulsedistro.service.AdaptService;
import com.example.pulsedistro.service.PipelineEventPublisher;
import com.example.pulsedistro.service.PlatformConfigService;
import com.example.pulsedistro.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "LANGCHAIN4J_API_KEY=",
        "LANGCHAIN4J_BASE_URL=http://localhost",
        "LANGCHAIN4J_MODEL_NAME=test-model"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ConfigAdaptStageTwoIntegrationTest {

    @Autowired
    private PlatformConfigService platformConfigService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private AdaptService adaptService;

    @Autowired
    private ContentTaskRepository taskRepository;

    @Autowired
    private PipelineEventPublisher eventPublisher;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @SpyBean
    private AdaptationModelClient modelClient;

    @Test
    void platformConfigsLoadDefaultLowercaseRules() {
        List<PlatformRule> rules = platformConfigService.listEnabledRules();

        assertThat(rules)
                .extracting(PlatformRule::platform)
                .containsExactly("xiaohongshu", "zhihu", "wechat", "bilibili");
        assertThat(platformConfigService.getRule("ZHihu").supportsMarkdown()).isTrue();
        assertThat(platformConfigService.getRule("xiaohongshu").image().maxCount()).isEqualTo(9);
    }

    @Test
    void adaptCreatesPlaceholdersThenTemplateFallbackProducesReadyRecords() {
        TaskSummaryResponse task = taskService.createTask(new CreateTaskRequest(
                "多平台分发练习",
                "MARKDOWN",
                "# 开场\n\n把一篇内容拆成适合每个平台的版本。\n\n- 保留重点\n- 调整语气"
        ));

        AdaptStartResponse started = adaptService.startAdaptation(task.taskId(), new AdaptRequest(
                List.of("xiaohongshu", "zhihu"),
                false
        ), "ut_stage2_service", "trace_stage2_service");

        assertThat(started.status()).isEqualTo("ADAPTING");
        assertThat(started.records())
                .extracting(record -> record.platform() + ":" + record.status())
                .containsExactly("xiaohongshu:ADAPTING", "zhihu:ADAPTING");

        await().untilAsserted(() -> assertThat(adaptService.listRecords(task.taskId()))
                .extracting(RecordSummaryResponse::status)
                .containsOnly("READY"));

        RecordDetailResponse detail = adaptService.getRecord(started.records().getFirst().recordId());

        assertThat(detail.publishMode()).isNull();
        assertThat(detail.adaptedTitle()).isNotBlank();
        assertThat(detail.adaptedContent()).contains("把一篇内容拆成适合每个平台的版本");
        assertThat(detail.tags()).isNotEmpty();
        assertThat(detail.styleExplanation()).contains("模板降级");
    }

    @Test
    void adaptApiRoutesCompletionAndDegradedEventsToUserTokenAndRestoresTaskStatus() throws Exception {
        String userToken = "ut_adapt_events";
        String traceId = "trace_adapt_events";
        TaskSummaryResponse task = taskService.createTask(new CreateTaskRequest(
                "事件路由任务",
                "MARKDOWN",
                "# 开场\n\n后台完成后前端不能卡在 loading。"
        ));

        mockMvc.perform(post("/api/tasks/{taskId}/adapt", task.taskId())
                        .header("X-User-Token", userToken)
                        .header("X-Trace-Id", traceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdaptRequest(
                                List.of("xiaohongshu", "zhihu"),
                                false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(202));

        await().untilAsserted(() -> {
            assertThat(adaptService.listRecords(task.taskId()))
                    .extracting(RecordSummaryResponse::status)
                    .containsOnly("READY");
            assertThat(taskRepository.findById(task.taskId()).orElseThrow().getStatus())
                    .isEqualTo("READY");
        });

        List<PipelineEvent> events = eventPublisher.recentEventsFor(userToken);
        assertThat(events).extracting(PipelineEvent::event)
                .contains("PLATFORM_ADAPT_COMPLETED", "PLATFORM_ADAPT_DEGRADED");
        assertThat(events).allSatisfy(event -> assertThat(event.data())
                .containsEntry("userToken", userToken)
                .containsEntry("taskId", task.taskId())
                .containsEntry("traceId", traceId));
        assertThat(eventPublisher.recentEventsFor("ut_other_user")).isEmpty();
    }

    @Test
    void adaptationRunsOnDedicatedAdaptExecutorThreads() {
        AtomicReference<String> threadName = new AtomicReference<>();
        doAnswer(invocation -> {
            threadName.set(Thread.currentThread().getName());
            return invocation.callRealMethod();
        }).when(modelClient).adapt(any(), any());

        TaskSummaryResponse task = taskService.createTask(new CreateTaskRequest(
                "线程池隔离任务",
                "MARKDOWN",
                "LLM I/O 不能占用 JVM 公共线程池。"
        ));

        adaptService.startAdaptation(task.taskId(), new AdaptRequest(
                List.of("xiaohongshu"),
                false
        ), "ut_adapt_executor", "trace_adapt_executor");

        await().untilAsserted(() -> {
            assertThat(threadName.get()).isNotBlank();
            assertThat(threadName.get()).startsWith("adapt-");
        });
    }

    @Test
    void taskFailedEventsUseStablePayloadShapeForPlatformAndTaskFailures() {
        doThrow(new IllegalStateException("model down")).when(modelClient).adapt(any(), any());
        TaskSummaryResponse task = taskService.createTask(new CreateTaskRequest(
                "失败事件结构",
                "MARKDOWN",
                "触发所有平台失败"
        ));

        adaptService.startAdaptation(task.taskId(), new AdaptRequest(
                List.of("xiaohongshu"),
                false
        ), "ut_failed_shape", "trace_failed_shape");

        await().untilAsserted(() -> assertThat(eventPublisher.recentEventsFor("ut_failed_shape").stream()
                .filter(event -> "TASK_FAILED".equals(event.event()))
                .toList()).hasSize(2));

        List<PipelineEvent> failedEvents = eventPublisher.recentEventsFor("ut_failed_shape").stream()
                .filter(event -> "TASK_FAILED".equals(event.event()))
                .toList();
        assertThat(failedEvents).allSatisfy(event -> assertThat(event.data())
                .containsKeys("scope", "userToken", "taskId", "traceId", "recordId", "platform", "status", "errorMessage"));
        assertThat(failedEvents)
                .extracting(event -> event.data().get("scope"))
                .containsExactlyInAnyOrder("platform", "task");
    }
}
