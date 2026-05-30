package com.example.pulsedistro.controller;

import com.example.pulsedistro.dto.adapt.RecordDetailResponse;
import com.example.pulsedistro.dto.adapt.RecordSummaryResponse;
import com.example.pulsedistro.dto.adapt.UpdateRecordRequest;
import com.example.pulsedistro.dto.common.ApiResponse;
import com.example.pulsedistro.service.AdaptService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class RecordController {

    private final AdaptService adaptService;

    public RecordController(AdaptService adaptService) {
        this.adaptService = adaptService;
    }

    @GetMapping("/api/tasks/{taskId}/records")
    public ApiResponse<List<RecordSummaryResponse>> list(@PathVariable String taskId) {
        return ApiResponse.success(adaptService.listRecords(taskId));
    }

    @GetMapping("/api/records/{recordId}")
    public ApiResponse<RecordDetailResponse> get(@PathVariable String recordId) {
        return ApiResponse.success(adaptService.getRecord(recordId));
    }

    @PutMapping("/api/records/{recordId}")
    public ApiResponse<RecordDetailResponse> update(
            @PathVariable String recordId,
            @RequestBody UpdateRecordRequest request
    ) {
        return ApiResponse.success(adaptService.updateRecord(recordId, request));
    }

    @PostMapping("/api/records/{recordId}/skip")
    public ApiResponse<RecordSummaryResponse> skip(@PathVariable String recordId) {
        return ApiResponse.success(adaptService.skipRecord(recordId));
    }
}
