package com.example.bookiibookii.domain.group.dto.req;

import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

public class ApplicationRequestDTO {
    // host의 수락/거절 상태 변화용
    @Getter
    public static class UpdateStatusDTO {
        @Schema(description = "변경할 상태 (ACCEPTED/REJECTED)", example = "ACCEPTED")
        @NotNull(message = "변경할 상태를 입력해주세요.")
        private ApplicationStatus status;
    }

    //게스트 그룹참여 신청서
    @Getter
    public static class JoinApplicationDTO {
        @Schema(description = "신청 한 마디", example = "안녕하세요! 끝까지 완독할 자신 있습니다.")
        @NotBlank(message = "신청 메시지를 입력해주세요.")
        @Size(max = 50, message = "신청 메시지는 50자 이내로 입력해주세요.") // 기획서의 0/50 반영
        private String applyMsg;
    }
}