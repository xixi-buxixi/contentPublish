package com.example.pulsedistro.controller;

import com.example.pulsedistro.dto.common.ApiResponse;
import com.example.pulsedistro.dto.task.CreateTaskRequest;
import com.example.pulsedistro.dto.task.TaskSummaryResponse;
import com.example.pulsedistro.model.NormalizedContent;
import com.example.pulsedistro.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ApiResponse<TaskSummaryResponse> createTask(@RequestBody CreateTaskRequest request) {
        return ApiResponse.success(taskService.createTask(request));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<TaskSummaryResponse> getTask(@PathVariable String taskId) {
        return ApiResponse.success(taskService.getTaskSummary(taskId));
    }

    @GetMapping("/{taskId}/normalized")
    public ApiResponse<NormalizedContent> getNormalized(@PathVariable String taskId) {
        return ApiResponse.success(taskService.getNormalizedContent(taskId));
    }

    @PutMapping("/{taskId}/normalized")
    public ApiResponse<NormalizedContent> updateNormalized(
            @PathVariable String taskId,
            @RequestBody NormalizedContent normalizedContent
    ) {
        return ApiResponse.success(taskService.updateNormalizedContent(taskId, normalizedContent));
    }
}
