package com.example.bookiibookii.global.apiPayload.handler;

import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.code.GeneralErrorCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;
import com.example.bookiibookii.global.notification.DiscordWebhookService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GeneralExceptionAdvice {

    private final DiscordWebhookService discordWebhookService;

    // 서비스 로직에서 의도적으로 발생시키는 예외
    // GeneralException을 상속한 모든 커스텀 예외 처리
    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(
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

    // 예상하지 못한 서버 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleException(
            Exception ex,
            HttpServletRequest request
    ) {
        discordWebhookService.sendUnexpectedExceptionAlert(request, ex);

        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        BaseCode code = GeneralErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(
                        code,
                        null
                ));
    }

    // 유저 입력값 검증 예외 처리
    // DTO 필드 단위 오류 메시지 반환 (@Valid, @Validated 실패)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidationException(
            MethodArgumentNotValidException ex
    ) {
        log.warn("Validation exception occurred", ex);

        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        BaseCode code = GeneralErrorCode.BAD_REQUEST;
        return ResponseEntity.status(code.getStatus())
                .body(ApiResponse.onFailure(
                        code,
                        errors
                ));
    }

    // 존재하지 않는 리소스 요청은 DEBUG 레벨로 로깅
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFound(NoResourceFoundException e) {
        log.debug("Resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not Found");
    }

}
