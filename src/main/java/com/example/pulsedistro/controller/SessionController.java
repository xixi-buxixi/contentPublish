package com.example.pulsedistro.controller;

import com.example.pulsedistro.dto.common.ApiResponse;
import com.example.pulsedistro.dto.session.SessionInitRequest;
import com.example.pulsedistro.dto.session.SessionInitResponse;
import com.example.pulsedistro.service.SessionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/init")
    public ApiResponse<SessionInitResponse> init(@RequestBody(required = false) SessionInitRequest request) {
        return ApiResponse.success(sessionService.init(request));
    }
}
