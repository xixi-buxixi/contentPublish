package com.example.pulsedistro.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "content_task")
public class ContentTask {

    @Id
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Lob
    @Column(name = "raw_content", nullable = false)
    private String rawContent;

    @Lob
    @Column(name = "normalized_content_json", nullable = false)
    private String normalizedContentJson;

    @Column(name = "cover_media_id")
    private String coverMediaId;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContentTask() {
    }

    public ContentTask(String title, String sourceType, String rawContent, String normalizedContentJson) {
        this.id = UUID.randomUUID().toString();
        this.title = title;
        this.sourceType = sourceType;
        this.rawContent = rawContent;
        this.normalizedContentJson = normalizedContentJson;
        this.status = "PENDING";
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getRawContent() {
        return rawContent;
    }

    public String getNormalizedContentJson() {
        return normalizedContentJson;
    }

    public void setNormalizedContentJson(String normalizedContentJson) {
        this.normalizedContentJson = normalizedContentJson;
    }

    public String getCoverMediaId() {
        return coverMediaId;
    }

    public void setCoverMediaId(String coverMediaId) {
        this.coverMediaId = coverMediaId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
