package com.example.pulsedistro.exception;

import com.example.pulsedistro.dto.common.ApiResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Map<String, Object>> handleBusinessException(BusinessException exception) {
        return ApiResponse.error(exception.getCode(), exception.getMessage(), Map.of());
    }

    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Map<String, Object>> handleRuntimeException(RuntimeException exception) {
        return ApiResponse.error(500, exception.getMessage(), Map.of());
    }
}
