package com.example.pulsedistro.controller;

import com.example.pulsedistro.dto.adapt.AdaptRequest;
import com.example.pulsedistro.dto.adapt.AdaptStartResponse;
import com.example.pulsedistro.dto.common.ApiResponse;
import com.example.pulsedistro.service.AdaptService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks/{taskId}/adapt")
public class AdaptController {

    private final AdaptService adaptService;

    public AdaptController(AdaptService adaptService) {
        this.adaptService = adaptService;
    }

    @PostMapping
    public ApiResponse<AdaptStartResponse> adapt(
            @PathVariable String taskId,
            @RequestBody AdaptRequest request,
            @RequestHeader(name = "X-User-Token", required = false) String userToken,
            @RequestHeader(name = "X-Trace-Id", required = false) String traceId
    ) {
        return ApiResponse.accepted(adaptService.startAdaptation(taskId, request, userToken, traceId));
    }
}
