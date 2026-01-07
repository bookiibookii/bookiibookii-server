package com.example.bookiibookii.global.apiPayload.handler;

import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.code.GeneralErrorCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GeneralExceptionAdvice {
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            GeneralException ex
    ) {
        log.warn("GeneralException occurred: code={}, message={}",
                ex.getCode().getCode(), ex.getCode().getMessage(), ex);
        return ResponseEntity.status(ex.getCode().getStatus())
                .body(ApiResponse.onFailure(
                                ex.getCode(),
                                null
                        )
                );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(
            Exception ex
    ) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        BaseCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(
                        code,
                        null
                ));
    }
}
