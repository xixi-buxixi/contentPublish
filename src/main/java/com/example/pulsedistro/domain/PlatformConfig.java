package com.example.pulsedistro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_config")
public class PlatformConfig {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String platform;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Lob
    @Column(name = "config_json", nullable = false)
    private String configJson;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PlatformConfig() {
    }

    public PlatformConfig(String platform, String displayName, String configJson, boolean enabled) {
        this.id = UUID.randomUUID().toString();
        this.platform = platform;
        this.displayName = displayName;
        this.configJson = configJson;
        this.enabled = enabled;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public String getPlatform() {
        return platform;
    }

    public String getConfigJson() {
        return configJson;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void update(String displayName, String configJson, boolean enabled) {
        this.displayName = displayName;
        this.configJson = configJson;
        this.enabled = enabled;
    }
}
