package com.example.pulsedistro.repository;

import com.example.pulsedistro.domain.PlatformPublishRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformPublishRecordRepository extends JpaRepository<PlatformPublishRecord, String> {

    List<PlatformPublishRecord> findByTaskIdOrderByCreatedAtAsc(String taskId);

    Optional<PlatformPublishRecord> findByTaskIdAndPlatform(String taskId, String platform);

    void deleteByTaskId(String taskId);
}
