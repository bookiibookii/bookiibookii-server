package com.example.bookiibookii.domain.group.dto.req;

import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

public class ApplicationRequestDTO {
    @Getter
    public static class UpdateStatusDTO {
        @Schema(description = "변경할 상태 (ACCEPTED/REJECTED)", example = "ACCEPTED")
        private ApplicationStatus status;
    }
}