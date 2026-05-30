package com.example.pulsedistro.repository;

import com.example.pulsedistro.domain.MediaResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MediaResourceRepository extends JpaRepository<MediaResource, String> {

    List<MediaResource> findByTaskIdOrderByCreatedAtAsc(String taskId);

    Optional<MediaResource> findByIdAndTaskId(String id, String taskId);
}
