package com.example.pulsedistro.service;

import com.example.pulsedistro.domain.MediaResource;
import com.example.pulsedistro.domain.PlatformPublishRecord;
import com.example.pulsedistro.dto.publish.PublishRequest;
import com.example.pulsedistro.dto.publish.PublishResponse;
import com.example.pulsedistro.dto.publish.PublishResultResponse;
import com.example.pulsedistro.exception.BusinessException;
import com.example.pulsedistro.model.MediaRef;
import com.example.pulsedistro.model.PlatformRule;
import com.example.pulsedistro.repository.ContentTaskRepository;
import com.example.pulsedistro.repository.MediaResourceRepository;
import com.example.pulsedistro.repository.PlatformPublishRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class PublishService {

    private final ContentTaskRepository taskRepository;
    private final PlatformPublishRecordRepository recordRepository;
    private final MediaResourceRepository mediaRepository;
    private final PlatformConfigService platformConfigService;
    private final PluginManager pluginManager;
    private final PipelineEventPublisher eventPublisher;
    private final JsonContentMapper jsonMapper;
    private final String publicBaseUrl;
    private final Path storageRoot;

    public PublishService(
            ContentTaskRepository taskRepository,
            PlatformPublishRecordRepository recordRepository,
            MediaResourceRepository mediaRepository,
            PlatformConfigService platformConfigService,
            PluginManager pluginManager,
            PipelineEventPublisher eventPublisher,
            JsonContentMapper jsonMapper,
            @Value("${pulse.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${pulse.media.storage-root:data/media}") String storageRoot
    ) {
        this.taskRepository = taskRepository;
        this.recordRepository = recordRepository;
        this.mediaRepository = mediaRepository;
        this.platformConfigService = platformConfigService;
        this.pluginManager = pluginManager;
        this.eventPublisher = eventPublisher;
        this.jsonMapper = jsonMapper;
        this.publicBaseUrl = publicBaseUrl;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Transactional
    public PublishResponse publish(String taskId, PublishRequest request, String userToken, String traceId) {
        if (!taskRepository.existsById(taskId)) {
            throw new BusinessException(404, "task not found");
        }

        String mode = normalizeMode(request == null ? null : request.mode());
        if ("real".equals(mode)) {
            if (!pluginManager.isOnline(userToken)) {
                throw new BusinessException(400, "请先开启您的浏览器分发插件");
            }
            return publishReal(taskId, request, userToken);
        }

        List<String> platforms = requestedPlatforms(request, taskId);
        List<PublishResultResponse> results = platforms.stream()
                .map(platform -> publishMock(taskId, platform, userToken))
                .toList();

        return new PublishResponse(taskId, mode, results);
    }

    private PublishResponse publishReal(String taskId, PublishRequest request, String userToken) {
        List<PublishResultResponse> results = requestedPlatforms(request, taskId).stream()
                .map(platform -> startRealPublish(taskId, platform, userToken))
                .toList();
        return new PublishResponse(taskId, "real", results);
    }

    private PublishResultResponse publishMock(String taskId, String platform, String userToken) {
        PlatformPublishRecord record = recordRepository.findByTaskIdAndPlatform(taskId, platform)
                .orElseThrow(() -> new BusinessException(404, "record not found"));
        validateReadyRecord(record);
        validateAgainstRule(record, platformConfigService.getRule(platform));
        validateMediaFiles(record);

        String publishUrl = normalizedBaseUrl() + "/mock/" + platform + "/" + record.getId();
        record.setPublishMode("mock");
        record.setPublishUrl(publishUrl);
        record.setPublishedAt(Instant.now());
        record.setStatus("SUCCESS");
        PlatformPublishRecord saved = recordRepository.save(record);

        eventPublisher.publish(userToken, "PUBLISH_STATUS_CHANGED", Map.of(
                "userToken", userToken,
                "taskId", taskId,
                "recordId", saved.getId(),
                "platform", platform,
                "status", saved.getStatus(),
                "publishUrl", publishUrl
        ));

        return new PublishResultResponse(saved.getId(), platform, saved.getStatus(), publishUrl);
    }

    private PublishResultResponse startRealPublish(String taskId, String platform, String userToken) {
        PlatformPublishRecord record = recordRepository.findByTaskIdAndPlatform(taskId, platform)
                .orElseThrow(() -> new BusinessException(404, "record not found"));
        validateReadyRecord(record);
        validateAgainstRule(record, platformConfigService.getRule(platform));
        validateMediaFiles(record);

        record.setPublishMode("real");
        record.setStatus("PUBLISHING");
        PlatformPublishRecord saved = recordRepository.save(record);

        eventPublisher.publish(userToken, "PUBLISH_STATUS_CHANGED", Map.of(
                "userToken", userToken,
                "taskId", taskId,
                "recordId", saved.getId(),
                "platform", platform,
                "status", saved.getStatus(),
                "publishUrl", ""
        ));

        return new PublishResultResponse(saved.getId(), platform, saved.getStatus(), saved.getPublishUrl());
    }

    private void validateReadyRecord(PlatformPublishRecord record) {
        if (!"READY".equals(record.getStatus())) {
            throw new BusinessException(409, "record is not ready");
        }
        if (!StringUtils.hasText(record.getAdaptedTitle()) || !StringUtils.hasText(record.getAdaptedContent())) {
            throw new BusinessException(400, "record content is incomplete");
        }
    }

    private void validateAgainstRule(PlatformPublishRecord record, PlatformRule rule) {
        if (record.getAdaptedTitle().length() > rule.maxTitleLength()) {
            throw new BusinessException(400, "title exceeds platform limit");
        }
        if (record.getAdaptedContent().length() > rule.maxContentLength()) {
            throw new BusinessException(400, "content exceeds platform limit");
        }
        if (jsonMapper.readTags(record.getTagsJson()).size() > rule.maxTags()) {
            throw new BusinessException(400, "too many tags");
        }
        if (jsonMapper.readMediaRefs(record.getAdaptedMediaJson()).size() > rule.image().maxCount()) {
            throw new BusinessException(400, "too many images");
        }
    }

    private void validateMediaFiles(PlatformPublishRecord record) {
        for (MediaRef mediaRef : jsonMapper.readMediaRefs(record.getAdaptedMediaJson())) {
            if (!StringUtils.hasText(mediaRef.mediaId())) {
                throw new BusinessException(400, "media must be uploaded before publish");
            }
            MediaResource media = mediaRepository.findByIdAndTaskId(mediaRef.mediaId(), record.getTaskId())
                    .orElseThrow(() -> new BusinessException(400, "media does not belong to task"));
            Path target = storageRoot.resolve(media.getStorageKey()).normalize();
            if (!target.startsWith(storageRoot)) {
                throw new BusinessException(400, "invalid media storage path");
            }
            if (!Files.isRegularFile(target)) {
                throw new BusinessException(400, "media file is not accessible");
            }
            validatePublicUrl(mediaRef, media);
        }
    }

    private void validatePublicUrl(MediaRef mediaRef, MediaResource media) {
        String expectedUrl = normalizedBaseUrl() + "/media/" + media.getId();
        if (!expectedUrl.equals(mediaRef.publicUrl())) {
            throw new BusinessException(400, "media publicUrl is not accessible");
        }
    }

    private List<String> requestedPlatforms(PublishRequest request, String taskId) {
        if (request == null || request.platforms() == null || request.platforms().isEmpty()) {
            return recordRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                    .map(PlatformPublishRecord::getPlatform)
                    .toList();
        }

        return request.platforms().stream()
                .map(this::normalizePlatform)
                .peek(platformConfigService::getRule)
                .distinct()
                .toList();
    }

    private String normalizeMode(String mode) {
        if (!StringUtils.hasText(mode)) {
            return "mock";
        }
        String normalized = mode.trim().toLowerCase(Locale.ROOT);
        if (!List.of("mock", "real").contains(normalized)) {
            throw new BusinessException(400, "unsupported publish mode");
        }
        return normalized;
    }

    private String normalizePlatform(String platform) {
        if (!StringUtils.hasText(platform)) {
            throw new BusinessException(400, "platform is required");
        }
        return platform.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizedBaseUrl() {
        if (publicBaseUrl.endsWith("/")) {
            return publicBaseUrl.substring(0, publicBaseUrl.length() - 1);
        }
        return publicBaseUrl;
    }
}
