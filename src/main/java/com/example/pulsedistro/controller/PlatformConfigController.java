package com.example.pulsedistro.controller;

import com.example.pulsedistro.dto.common.ApiResponse;
import com.example.pulsedistro.model.PlatformRule;
import com.example.pulsedistro.service.PlatformConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/configs/platforms")
public class PlatformConfigController {

    private final PlatformConfigService platformConfigService;

    public PlatformConfigController(PlatformConfigService platformConfigService) {
        this.platformConfigService = platformConfigService;
    }

    @GetMapping
    public ApiResponse<List<PlatformRule>> list() {
        return ApiResponse.success(platformConfigService.listEnabledRules());
    }

    @GetMapping("/{platform}")
    public ApiResponse<PlatformRule> get(@PathVariable String platform) {
        return ApiResponse.success(platformConfigService.getRule(platform));
    }
}
