package com.example.bookiibookii.domain.group.dto.res;


import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
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
    public static class ApplicationListDTO{
        @Builder.Default
        private List<ApplicationDetailDTO> applicationList = new ArrayList<>();
        private Integer totalCount;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class ApplicationDetailDTO {
        private Long applicationId;
        private Long userId;
        private String name;
        //private String profileImageUrl; //유저 프로필 이미지
        //@Builder.Default
        //private List<String> tags = new ArrayList<>(); // "#메모환영", "#인사이트" 등?
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
        private String updatedAt;         // 변경된 시간 (포맷팅된 문자열)
    }

}
