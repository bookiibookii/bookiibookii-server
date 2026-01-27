package com.example.bookiibookii.domain.group.dto.res;


import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class ApplicationResponseDTO {


    @Getter
    @Builder
    @AllArgsConstructor
    public static class ApplicationListDTO {
        @Builder.Default
        private List<ApplicationDetailDTO> applicationList = new ArrayList<>();
        private Integer totalCount;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ApplicationDetailDTO {
        private Long applicationId;
        private Long user;
        private String name;
        //private String profileImageUrl; //유저 프로필 이미지
        private List<String> tags; // "#메모환영", "#인사이트" 등?
        private String createdAt;
        private String applyMsg;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpdateResultDTO {
        private Long applicationId;      // 수정된 신청 ID
        private ApplicationStatus status; // 변경된 상태
        private GroupStatus groupStatus; //그룹상태(RECRUTING, MATCHED)
        private String updatedAt;         // 변경된 시간 (포맷팅된 문자열)
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinResultDTO {
        private Long applicationId;  // 신청서 ID
        private String status;       // 신청 상태 (PENDING 등)
        private String createdAt;    // 신청 일자
    }
}
