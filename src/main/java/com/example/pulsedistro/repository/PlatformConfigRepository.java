package com.example.pulsedistro.repository;

import com.example.pulsedistro.domain.PlatformConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlatformConfigRepository extends JpaRepository<PlatformConfig, String> {

    Optional<PlatformConfig> findByPlatform(String platform);

    List<PlatformConfig> findByEnabledTrue();
}
