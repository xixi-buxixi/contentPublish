package com.example.pulsedistro.controller;

import com.example.pulsedistro.dto.common.ApiResponse;
import com.example.pulsedistro.dto.publish.PublishRequest;
import com.example.pulsedistro.dto.publish.PublishResponse;
import com.example.pulsedistro.service.PublishService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/publish")
public class PublishController {

    private final PublishService publishService;

    public PublishController(PublishService publishService) {
        this.publishService = publishService;
    }

    @PostMapping
    public ApiResponse<PublishResponse> publish(
            @PathVariable String taskId,
            @RequestBody PublishRequest request,
            @RequestHeader(value = "X-User-Token", defaultValue = "anonymous") String userToken,
            @RequestHeader(value = "X-Trace-Id", defaultValue = "") String traceId
    ) {
        return ApiResponse.success(publishService.publish(taskId, request, userToken, traceId));
    }
}
