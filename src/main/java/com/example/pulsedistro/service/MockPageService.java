package com.example.pulsedistro.service;

import com.example.pulsedistro.domain.PlatformPublishRecord;
import com.example.pulsedistro.domain.ContentTask;
import com.example.pulsedistro.dto.publish.MockPageDataResponse;
import com.example.pulsedistro.exception.BusinessException;
import com.example.pulsedistro.model.NormalizedContent;
import com.example.pulsedistro.repository.ContentTaskRepository;
import com.example.pulsedistro.repository.PlatformPublishRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MockPageService {

    private final PlatformPublishRecordRepository recordRepository;
    private final ContentTaskRepository taskRepository;
    private final JsonContentMapper jsonMapper;

    public MockPageService(
            PlatformPublishRecordRepository recordRepository,
            ContentTaskRepository taskRepository,
            JsonContentMapper jsonMapper
    ) {
        this.recordRepository = recordRepository;
        this.taskRepository = taskRepository;
        this.jsonMapper = jsonMapper;
    }

    public MockPageDataResponse getMockData(String platform, String recordId) {
        PlatformPublishRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(404, "record not found"));
        if (!record.getPlatform().equals(platform)) {
            throw new BusinessException(404, "mock page not found");
        }
        if (!"SUCCESS".equals(record.getStatus())) {
            throw new BusinessException(409, "record is not published");
        }

        return new MockPageDataResponse(
                record.getId(),
                record.getPlatform(),
                record.getAdaptedTitle(),
                summaryFor(record),
                record.getAdaptedContent(),
                jsonMapper.readTags(record.getTagsJson()),
                jsonMapper.readMediaRefs(record.getAdaptedMediaJson()),
                record.getPublishedAt()
        );
    }

    private String summaryFor(PlatformPublishRecord record) {
        String normalizedSummary = taskRepository.findById(record.getTaskId())
                .map(ContentTask::getNormalizedContentJson)
                .map(jsonMapper::readNormalized)
                .map(NormalizedContent::summary)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse("");
        if (StringUtils.hasText(normalizedSummary)) {
            return normalizedSummary;
        }
        return firstParagraph(record.getAdaptedContent());
    }

    private String firstParagraph(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse("")
                .replaceAll("\\s+", " ");
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120) + "...";
    }
}
