package com.example.bookiibookii.domain.support.dto.req;

import com.example.bookiibookii.domain.support.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReportRequestDTO {
    // 신고 생성 요청 DTO
    public record CreateReportDTO(
            @NotNull
            Long groupId,
            @NotNull
            Long targetId,
            @NotNull
            ReportType reportType,

            @NotBlank(message = "구체적인 상황을 설명해주세요.")
            @Size(max = 50, message = "내용은 1000자 이내로 입력해주세요.")
            String content
    ) {}
}
