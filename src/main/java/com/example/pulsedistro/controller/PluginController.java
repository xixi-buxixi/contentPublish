package com.example.pulsedistro.controller;

import com.example.pulsedistro.dto.common.ApiResponse;
import com.example.pulsedistro.dto.plugin.PluginHeartbeatRequest;
import com.example.pulsedistro.dto.plugin.PluginPublishStatusRequest;
import com.example.pulsedistro.dto.plugin.PluginPublishStatusResponse;
import com.example.pulsedistro.dto.plugin.PluginRegisterRequest;
import com.example.pulsedistro.dto.plugin.PluginSessionResponse;
import com.example.pulsedistro.dto.plugin.PluginStatusResponse;
import com.example.pulsedistro.service.PluginManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plugin")
public class PluginController {

    private final PluginManager pluginManager;

    public PluginController(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @PostMapping("/register")
    public ApiResponse<PluginSessionResponse> register(
            @RequestHeader("X-User-Token") String userToken,
            @RequestBody PluginRegisterRequest request
    ) {
        return ApiResponse.success(pluginManager.register(userToken, request));
    }

    @PostMapping("/heartbeat")
    public ApiResponse<PluginSessionResponse> heartbeat(
            @RequestHeader("X-User-Token") String userToken,
            @RequestBody PluginHeartbeatRequest request
    ) {
        return ApiResponse.success(pluginManager.heartbeat(userToken, request));
    }

    @GetMapping("/status")
    public ApiResponse<PluginStatusResponse> status(@RequestHeader("X-User-Token") String userToken) {
        return ApiResponse.success(pluginManager.status(userToken));
    }

    @PostMapping("/publish-status")
    public ApiResponse<PluginPublishStatusResponse> publishStatus(
            @RequestHeader("X-User-Token") String userToken,
            @RequestBody PluginPublishStatusRequest request
    ) {
        return ApiResponse.success(pluginManager.reportPublishStatus(userToken, request));
    }
}
