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
@Table(name = "platform_publish_record")
public class PlatformPublishRecord {

    @Id
    private String id;

    @Column(name = "task_id", nullable = false)
    private String taskId;

    @Column(nullable = false)
    private String platform;

    @Column(name = "adapted_title")
    private String adaptedTitle;

    @Lob
    @Column(name = "adapted_content")
    private String adaptedContent;

    @Lob
    @Column(name = "tags_json", nullable = false)
    private String tagsJson;

    @Lob
    @Column(name = "adapted_media_json", nullable = false)
    private String adaptedMediaJson;

    @Column(name = "publish_mode")
    private String publishMode;

    @Column(nullable = false)
    private String status;

    @Column(name = "publish_url")
    private String publishUrl;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "style_explanation")
    private String styleExplanation;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected PlatformPublishRecord() {
    }

    public PlatformPublishRecord(String taskId, String platform) {
        this.id = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.platform = platform;
        this.tagsJson = "[]";
        this.adaptedMediaJson = "[]";
        this.status = "ADAPTING";
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

    public String getTaskId() {
        return taskId;
    }

    public String getPlatform() {
        return platform;
    }

    public String getAdaptedTitle() {
        return adaptedTitle;
    }

    public String getAdaptedContent() {
        return adaptedContent;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public String getAdaptedMediaJson() {
        return adaptedMediaJson;
    }

    public String getPublishMode() {
        return publishMode;
    }

    public void setPublishMode(String publishMode) {
        this.publishMode = publishMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPublishUrl() {
        return publishUrl;
    }

    public void setPublishUrl(String publishUrl) {
        this.publishUrl = publishUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getStyleExplanation() {
        return styleExplanation;
    }

    public void setStyleExplanation(String styleExplanation) {
        this.styleExplanation = styleExplanation;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void markReady(String title, String content, String tagsJson, String mediaJson, String styleExplanation) {
        this.adaptedTitle = title;
        this.adaptedContent = content;
        this.tagsJson = tagsJson;
        this.adaptedMediaJson = mediaJson;
        this.styleExplanation = styleExplanation;
        this.errorMessage = null;
        this.status = "READY";
    }

    public void markFailed(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = "FAILED";
    }

    public void updateEditedContent(String title, String content, String tagsJson, String mediaJson) {
        this.adaptedTitle = title;
        this.adaptedContent = content;
        this.tagsJson = tagsJson;
        this.adaptedMediaJson = mediaJson;
        this.status = "READY";
    }
}
