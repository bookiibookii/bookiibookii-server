package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.tracker.enums.TrackerTopBannerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "트래커 메인 상단 배너")
public record TrackerTopBannerResponse(
        TrackerTopBannerType bannerType,
        Long groupId,
        Long matchedMemberId,
        String groupName,
        String partnerNickname,
        String bookTitle,
        String title,
        String titleTemplate,
        String subtitle,
        String dDayLabel,
        LocalDateTime targetAt,
        Long remainingSeconds
) {
}
