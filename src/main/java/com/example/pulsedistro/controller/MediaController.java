package com.example.pulsedistro.controller;

import com.example.pulsedistro.dto.common.ApiResponse;
import com.example.pulsedistro.dto.media.MediaDeleteResponse;
import com.example.pulsedistro.dto.media.MediaUploadResponse;
import com.example.pulsedistro.service.MediaService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class MediaController {

    private final MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/api/tasks/{taskId}/media")
    public ApiResponse<MediaUploadResponse> upload(
            @PathVariable String taskId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "alt", required = false) String alt
    ) {
        return ApiResponse.success(mediaService.store(taskId, file, alt));
    }

    @GetMapping("/api/tasks/{taskId}/media")
    public ApiResponse<List<MediaUploadResponse>> list(@PathVariable String taskId) {
        return ApiResponse.success(mediaService.listByTask(taskId));
    }

    @DeleteMapping("/api/tasks/{taskId}/media/{mediaId}")
    public ApiResponse<MediaDeleteResponse> delete(@PathVariable String taskId, @PathVariable String mediaId) {
        mediaService.deleteMedia(taskId, mediaId);
        return ApiResponse.success(new MediaDeleteResponse(mediaId, true));
    }

    @GetMapping("/media/{mediaId}")
    public ResponseEntity<FileSystemResource> load(@PathVariable String mediaId) {
        MediaService.MediaFile file = mediaService.loadFile(mediaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .body(new FileSystemResource(file.path()));
    }
}
