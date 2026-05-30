package com.example.pulsedistro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_resource")
public class MediaResource {

    @Id
    private String id;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    private Integer width;

    private Integer height;

    @Column(name = "storage_type", nullable = false)
    private String storageType;

    @Column(name = "storage_key", nullable = false)
    private String storageKey;

    @Column(name = "public_url", nullable = false)
    private String publicUrl;

    @Column(nullable = false)
    private String sha256;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected MediaResource() {
    }

    public MediaResource(
            String taskId,
            String originalName,
            String mimeType,
            long sizeBytes,
            Integer width,
            Integer height,
            String storageKey,
            String publicUrl,
            String sha256
    ) {
        this.id = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.originalName = originalName;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.width = width;
        this.height = height;
        this.storageType = "local";
        this.storageKey = storageKey;
        this.publicUrl = publicUrl;
        this.sha256 = sha256;
        this.status = "READY";
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getPublicUrl() {
        return publicUrl;
    }

    public void setPublicUrl(String publicUrl) {
        this.publicUrl = publicUrl;
    }

    public String getStatus() {
        return status;
    }
}
