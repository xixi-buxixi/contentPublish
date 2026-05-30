package com.example.pulsedistro.service;

import com.example.pulsedistro.domain.ContentTask;
import com.example.pulsedistro.domain.MediaResource;
import com.example.pulsedistro.domain.PlatformPublishRecord;
import com.example.pulsedistro.dto.adapt.AdaptRecordPlaceholder;
import com.example.pulsedistro.dto.adapt.AdaptRequest;
import com.example.pulsedistro.dto.adapt.AdaptStartResponse;
import com.example.pulsedistro.dto.adapt.RecordDetailResponse;
import com.example.pulsedistro.dto.adapt.RecordSummaryResponse;
import com.example.pulsedistro.dto.adapt.UpdateRecordRequest;
import com.example.pulsedistro.exception.BusinessException;
import com.example.pulsedistro.model.MediaRef;
import com.example.pulsedistro.model.NormalizedContent;
import com.example.pulsedistro.model.PlatformRule;
import com.example.pulsedistro.repository.ContentTaskRepository;
import com.example.pulsedistro.repository.MediaResourceRepository;
import com.example.pulsedistro.repository.PlatformPublishRecordRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class AdaptService {

    private final ContentTaskRepository taskRepository;
    private final MediaResourceRepository mediaRepository;
    private final PlatformPublishRecordRepository recordRepository;
    private final PlatformConfigService platformConfigService;
    private final AdaptationModelClient modelClient;
    private final PipelineEventPublisher eventPublisher;
    private final JsonContentMapper jsonMapper;
    private final Executor adaptTaskExecutor;

    public AdaptService(
            ContentTaskRepository taskRepository,
            MediaResourceRepository mediaRepository,
            PlatformPublishRecordRepository recordRepository,
            PlatformConfigService platformConfigService,
            AdaptationModelClient modelClient,
            PipelineEventPublisher eventPublisher,
            JsonContentMapper jsonMapper,
            @Qualifier("adaptTaskExecutor") Executor adaptTaskExecutor
    ) {
        this.taskRepository = taskRepository;
        this.mediaRepository = mediaRepository;
        this.recordRepository = recordRepository;
        this.platformConfigService = platformConfigService;
        this.modelClient = modelClient;
        this.eventPublisher = eventPublisher;
        this.jsonMapper = jsonMapper;
        this.adaptTaskExecutor = adaptTaskExecutor;
    }

    public AdaptStartResponse startAdaptation(String taskId, AdaptRequest request, String userToken, String traceId) {
        String normalizedUserToken = requireText(userToken, "userToken is required");
        String normalizedTraceId = StringUtils.hasText(traceId) ? traceId.trim() : "";
        ContentTask task = getTask(taskId);
        NormalizedContent normalized = jsonMapper.readNormalized(task.getNormalizedContentJson());
        List<String> platforms = requestedPlatforms(request);
        List<AdaptRecordPlaceholder> placeholders = new ArrayList<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        task.setStatus("ADAPTING");
        taskRepository.save(task);

        for (String platform : platforms) {
            PlatformPublishRecord record = recordRepository.findByTaskIdAndPlatform(taskId, platform)
                    .orElseGet(() -> new PlatformPublishRecord(taskId, platform));
            record.setStatus("ADAPTING");
            PlatformPublishRecord saved = recordRepository.save(record);
            placeholders.add(new AdaptRecordPlaceholder(platform, saved.getId(), saved.getStatus()));

            PlatformRule rule = platformConfigService.getRule(platform);
            futures.add(CompletableFuture.runAsync(() ->
                    adaptRecord(saved.getId(), normalized, rule, normalizedUserToken, normalizedTraceId), adaptTaskExecutor));
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .whenComplete((ignored, error) -> refreshTaskStatus(taskId, normalizedUserToken, normalizedTraceId));

        return new AdaptStartResponse(taskId, "ADAPTING", List.copyOf(placeholders), "平台适配已开始，请监听 WebSocket。");
    }

    public List<RecordSummaryResponse> listRecords(String taskId) {
        return recordRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(this::toSummary)
                .toList();
    }

    public RecordDetailResponse getRecord(String recordId) {
        return toDetail(getRecordEntity(recordId));
    }

    public RecordDetailResponse updateRecord(String recordId, UpdateRecordRequest request) {
        PlatformPublishRecord record = getRecordEntity(recordId);
        List<MediaRef> media = request.mediaIds() == null
                ? List.of()
                : request.mediaIds().stream()
                .map(mediaId -> mediaRepository.findByIdAndTaskId(mediaId, record.getTaskId())
                        .orElseThrow(() -> new BusinessException(400, "media does not belong to task")))
                .map(this::toMediaRef)
                .toList();

        record.updateEditedContent(
                requireText(request.adaptedTitle(), "adaptedTitle is required"),
                request.adaptedContent() == null ? "" : request.adaptedContent(),
                jsonMapper.write(request.tags() == null ? List.of() : request.tags()),
                jsonMapper.write(media)
        );
        return toDetail(recordRepository.save(record));
    }

    public RecordSummaryResponse skipRecord(String recordId) {
        PlatformPublishRecord record = getRecordEntity(recordId);
        record.setStatus("SKIPPED");
        return toSummary(recordRepository.save(record));
    }

    private void adaptRecord(
            String recordId,
            NormalizedContent normalized,
            PlatformRule rule,
            String userToken,
            String traceId
    ) {
        try {
            PlatformPublishRecord record = getRecordEntity(recordId);
            AdaptationModelClient.AdaptationResult result = modelClient.adapt(normalized, rule);
            record.markReady(
                    result.title(),
                    result.content(),
                    jsonMapper.write(result.tags()),
                    jsonMapper.write(result.media()),
                    result.styleExplanation()
            );
            PlatformPublishRecord saved = recordRepository.save(record);

            if (result.degraded()) {
                publishPlatformEvent(userToken, "PLATFORM_ADAPT_DEGRADED", saved, traceId);
            }
            publishPlatformEvent(userToken, "PLATFORM_ADAPT_COMPLETED", saved, traceId);
        } catch (RuntimeException e) {
            recordRepository.findById(recordId).ifPresent(record -> {
                record.markFailed(StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName());
                PlatformPublishRecord saved = recordRepository.save(record);
                publishTaskFailedEvent(
                        userToken,
                        traceId,
                        "platform",
                        saved.getTaskId(),
                        saved.getId(),
                        saved.getPlatform(),
                        saved.getStatus(),
                        saved.getErrorMessage()
                );
            });
        }
    }

    private void refreshTaskStatus(String taskId, String userToken, String traceId) {
        List<PlatformPublishRecord> records = recordRepository.findByTaskIdOrderByCreatedAtAsc(taskId);
        if (records.isEmpty() || records.stream().anyMatch(record -> "ADAPTING".equals(record.getStatus()))) {
            return;
        }

        ContentTask task = getTask(taskId);
        boolean allFailed = records.stream().allMatch(record -> "FAILED".equals(record.getStatus()));
        task.setStatus(allFailed ? "FAILED" : "READY");
        ContentTask saved = taskRepository.save(task);

        if (allFailed) {
            publishTaskFailedEvent(
                    userToken,
                    traceId,
                    "task",
                    saved.getId(),
                    "",
                    "",
                    saved.getStatus(),
                    "all platform adaptations failed"
            );
        }
    }

    private void publishPlatformEvent(
            String userToken,
            String eventName,
            PlatformPublishRecord record,
            String traceId
    ) {
        Map<String, Object> data = baseData(userToken, record.getTaskId(), traceId);
        data.put("recordId", record.getId());
        data.put("platform", record.getPlatform());
        data.put("status", record.getStatus());
        data.put("errorMessage", record.getErrorMessage() == null ? "" : record.getErrorMessage());
        eventPublisher.publish(userToken, eventName, data);
    }

    private void publishTaskFailedEvent(
            String userToken,
            String traceId,
            String scope,
            String taskId,
            String recordId,
            String platform,
            String status,
            String errorMessage
    ) {
        Map<String, Object> data = baseData(userToken, taskId, traceId);
        data.put("scope", scope);
        data.put("recordId", recordId == null ? "" : recordId);
        data.put("platform", platform == null ? "" : platform);
        data.put("status", status == null ? "" : status);
        data.put("errorMessage", errorMessage == null ? "" : errorMessage);
        eventPublisher.publish(userToken, "TASK_FAILED", data);
    }

    private Map<String, Object> baseData(String userToken, String taskId, String traceId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userToken", userToken);
        data.put("taskId", taskId);
        data.put("traceId", traceId == null ? "" : traceId);
        return data;
    }

    private List<String> requestedPlatforms(AdaptRequest request) {
        if (request == null || request.platforms() == null || request.platforms().isEmpty()) {
            return platformConfigService.listEnabledRules().stream()
                    .map(PlatformRule::platform)
                    .toList();
        }

        return request.platforms().stream()
                .map(this::normalizePlatform)
                .peek(platformConfigService::getRule)
                .distinct()
                .toList();
    }

    private String normalizePlatform(String platform) {
        if (!StringUtils.hasText(platform)) {
            throw new BusinessException(400, "platform is required");
        }
        return platform.trim().toLowerCase(Locale.ROOT);
    }

    private ContentTask getTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(404, "task not found"));
    }

    private PlatformPublishRecord getRecordEntity(String recordId) {
        return recordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(404, "record not found"));
    }

    private String requireText(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(400, message);
        }
        return value.trim();
    }

    private MediaRef toMediaRef(MediaResource media) {
        return new MediaRef(
                media.getId(),
                media.getPublicUrl(),
                media.getOriginalName(),
                media.getWidth(),
                media.getHeight()
        );
    }

    private RecordSummaryResponse toSummary(PlatformPublishRecord record) {
        return new RecordSummaryResponse(
                record.getId(),
                record.getTaskId(),
                record.getPlatform(),
                record.getAdaptedTitle(),
                record.getStatus(),
                record.getPublishMode(),
                record.getPublishUrl()
        );
    }

    private RecordDetailResponse toDetail(PlatformPublishRecord record) {
        return new RecordDetailResponse(
                record.getId(),
                record.getTaskId(),
                record.getPlatform(),
                record.getAdaptedTitle(),
                record.getAdaptedContent(),
                jsonMapper.readTags(record.getTagsJson()),
                jsonMapper.readMediaRefs(record.getAdaptedMediaJson()),
                record.getStyleExplanation(),
                record.getStatus(),
                record.getPublishMode(),
                record.getPublishUrl(),
                record.getPublishedAt()
        );
    }
}
