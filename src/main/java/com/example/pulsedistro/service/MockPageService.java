package com.example.pulsedistro.service;

import com.example.pulsedistro.domain.PlatformPublishRecord;
import com.example.pulsedistro.dto.publish.MockPageDataResponse;
import com.example.pulsedistro.exception.BusinessException;
import com.example.pulsedistro.repository.PlatformPublishRecordRepository;
import org.springframework.stereotype.Service;

@Service
public class MockPageService {

    private final PlatformPublishRecordRepository recordRepository;
    private final JsonContentMapper jsonMapper;

    public MockPageService(PlatformPublishRecordRepository recordRepository, JsonContentMapper jsonMapper) {
        this.recordRepository = recordRepository;
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
                record.getAdaptedContent(),
                jsonMapper.readTags(record.getTagsJson()),
                jsonMapper.readMediaRefs(record.getAdaptedMediaJson()),
                record.getPublishedAt()
        );
    }
}
