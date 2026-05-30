package com.example.pulsedistro.service;

import com.example.pulsedistro.domain.ContentTask;
import com.example.pulsedistro.domain.MediaResource;
import com.example.pulsedistro.domain.PlatformPublishRecord;
import com.example.pulsedistro.dto.media.MediaUploadResponse;
import com.example.pulsedistro.exception.BusinessException;
import com.example.pulsedistro.model.ContentBlock;
import com.example.pulsedistro.model.MediaRef;
import com.example.pulsedistro.model.NormalizedContent;
import com.example.pulsedistro.repository.ContentTaskRepository;
import com.example.pulsedistro.repository.MediaResourceRepository;
import com.example.pulsedistro.repository.PlatformPublishRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class MediaService {

    private final ContentTaskRepository taskRepository;
    private final MediaResourceRepository mediaRepository;
    private final PlatformPublishRecordRepository recordRepository;
    private final JsonContentMapper jsonMapper;
    private final Path storageRoot;
    private final String publicBaseUrl;

    public MediaService(
            ContentTaskRepository taskRepository,
            MediaResourceRepository mediaRepository,
            PlatformPublishRecordRepository recordRepository,
            JsonContentMapper jsonMapper,
            @Value("${pulse.media.storage-root:data/media}") String storageRoot,
            @Value("${pulse.public-base-url:http://localhost:8080}") String publicBaseUrl
    ) {
        this.taskRepository = taskRepository;
        this.mediaRepository = mediaRepository;
        this.recordRepository = recordRepository;
        this.jsonMapper = jsonMapper;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
    }

    @Transactional
    public MediaUploadResponse store(String taskId, MultipartFile file, String alt) {
        requireTask(taskId);
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "file is required");
        }

        String mimeType = requireImageMimeType(file.getContentType());
        byte[] bytes = readBytes(file);
        String sha256 = sha256(bytes);
        String extension = extension(file.getOriginalFilename(), mimeType);
        String storageKey = taskId + "/" + sha256 + extension;
        Path target = storageRoot.resolve(storageKey).normalize();
        ensureInsideStorage(target);
        writeFile(target, bytes);

        ImageSize imageSize = detectImageSize(bytes);
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename()
                : "upload" + extension;
        MediaResource media = new MediaResource(
                taskId,
                originalName,
                mimeType,
                bytes.length,
                imageSize.width(),
                imageSize.height(),
                storageKey,
                "",
                sha256
        );
        media.setPublicUrl(normalizedBaseUrl() + "/media/" + media.getId());
        MediaResource saved = mediaRepository.save(media);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<MediaUploadResponse> listByTask(String taskId) {
        requireTask(taskId);
        return mediaRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteMedia(String taskId, String mediaId) {
        MediaResource media = mediaRepository.findByIdAndTaskId(mediaId, taskId)
                .orElseThrow(() -> new BusinessException(404, "media not found"));

        ContentTask task = requireTask(taskId);
        if (normalizedContentReferences(task, mediaId)) {
            throw new BusinessException(409, "media is referenced by normalized content");
        }
        if (publishRecordReferences(taskId, mediaId)) {
            throw new BusinessException(409, "media is referenced by publish record");
        }

        mediaRepository.delete(media);
        deleteFile(media.getStorageKey());
    }

    @Transactional(readOnly = true)
    public MediaFile loadFile(String mediaId) {
        MediaResource media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new BusinessException(404, "media not found"));
        Path target = storageRoot.resolve(media.getStorageKey()).normalize();
        ensureInsideStorage(target);
        if (!Files.exists(target)) {
            throw new BusinessException(404, "media file not found");
        }
        return new MediaFile(target, media.getMimeType());
    }

    private ContentTask requireTask(String taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException(404, "task not found"));
    }

    private String requireImageMimeType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            throw new BusinessException(400, "image content type is required");
        }

        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        if (!List.of("image/jpeg", "image/png", "image/webp").contains(normalized)) {
            throw new BusinessException(400, "unsupported image content type");
        }
        return normalized;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private String extension(String originalName, String mimeType) {
        if (StringUtils.hasText(originalName)) {
            String lower = originalName.toLowerCase(Locale.ROOT);
            int dot = lower.lastIndexOf('.');
            if (dot >= 0 && dot < lower.length() - 1) {
                String ext = lower.substring(dot);
                if (List.of(".jpg", ".jpeg", ".png", ".webp").contains(ext)) {
                    return ext;
                }
            }
        }

        return switch (mimeType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }

    private void writeFile(Path target, byte[] bytes) {
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void deleteFile(String storageKey) {
        Path target = storageRoot.resolve(storageKey).normalize();
        ensureInsideStorage(target);
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void ensureInsideStorage(Path target) {
        if (!target.startsWith(storageRoot)) {
            throw new BusinessException(400, "invalid storage path");
        }
    }

    private ImageSize detectImageSize(byte[] bytes) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            if (image == null) {
                return new ImageSize(null, null);
            }
            return new ImageSize(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            return new ImageSize(null, null);
        }
    }

    private boolean normalizedContentReferences(ContentTask task, String mediaId) {
        NormalizedContent normalized = jsonMapper.readNormalized(task.getNormalizedContentJson());
        if (normalized.blocks() == null) {
            return false;
        }
        return normalized.blocks().stream().anyMatch(block -> referencesMedia(block, mediaId));
    }

    private boolean referencesMedia(ContentBlock block, String mediaId) {
        return block.media() != null && mediaId.equals(block.media().mediaId());
    }

    private boolean publishRecordReferences(String taskId, String mediaId) {
        for (PlatformPublishRecord record : recordRepository.findByTaskIdOrderByCreatedAtAsc(taskId)) {
            List<MediaRef> mediaRefs = jsonMapper.readMediaRefs(record.getAdaptedMediaJson());
            boolean referenced = mediaRefs.stream().anyMatch(mediaRef -> mediaId.equals(mediaRef.mediaId()));
            if (referenced) {
                return true;
            }
        }
        return false;
    }

    private String normalizedBaseUrl() {
        if (publicBaseUrl.endsWith("/")) {
            return publicBaseUrl.substring(0, publicBaseUrl.length() - 1);
        }
        return publicBaseUrl;
    }

    private MediaUploadResponse toResponse(MediaResource media) {
        return new MediaUploadResponse(
                media.getId(),
                media.getTaskId(),
                media.getPublicUrl(),
                media.getMimeType(),
                media.getSizeBytes(),
                media.getWidth(),
                media.getHeight(),
                media.getStatus()
        );
    }

    private record ImageSize(Integer width, Integer height) {
    }

    public record MediaFile(Path path, String mimeType) {
    }
}
