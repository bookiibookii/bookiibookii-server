package com.example.bookiibookii.domain.tracker.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TrackerReceiveRequestDTO(
        @Schema(description = "수령 인증 이미지 S3 키", example = "image/trackers/d6239b1e-dcd8-483c-baaf-018da2fa1328")
        @NotBlank(message = "수령 인증 이미지 S3 키는 필수 입력입니다. Presigned URL로 업로드 후 발급받은 s3Key를 전달하세요.")
        String s3Key
) {}
