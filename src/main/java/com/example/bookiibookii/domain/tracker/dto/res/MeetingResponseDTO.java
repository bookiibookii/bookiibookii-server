package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Schema(description = "직접 교환 약속 상세 응답")
public record MeetingResponseDTO(
        @Schema(description = "Meeting ID", example = "10")
        Long meetingId,
        @Schema(description = "교환 라운드", example = "FIRST_EXCHANGE")
        ExchangeRound exchangeRound,
        @Schema(description = "약속 장소 정보")
        LocationInfo location,
        @Schema(description = "상세 주소", example = "2층 창가 자리")
        String addressDetail,
        @Schema(description = "약속 일시", example = "2026-05-20T14:30:00")
        LocalDateTime scheduledAt,
        @Schema(description = "약속 등록자 정보")
        CreatedByInfo createdBy
) {

    public static MeetingResponseDTO from(Meeting meeting) {
        return MeetingResponseDTO.builder()
                .meetingId(meeting.getId())
                .exchangeRound(meeting.getExchangeRound())
                .location(LocationInfo.from(meeting))
                .addressDetail(meeting.getAddressDetail())
                .scheduledAt(meeting.getScheduledAt())
                .createdBy(CreatedByInfo.from(meeting))
                .build();
    }

    @Builder
    @Schema(description = "약속 장소 정보")
    public record LocationInfo(
            @Schema(description = "장소명", example = "강남역")
            String placeName,
            @Schema(description = "주소", example = "서울특별시 강남구 강남대로 396")
            String address,
            @Schema(description = "우편번호", example = "06232")
            String zipCode,
            @Schema(description = "X 좌표(경도)", example = "127.027621")
            BigDecimal x,
            @Schema(description = "Y 좌표(위도)", example = "37.497942")
            BigDecimal y
    ) {

        private static LocationInfo from(Meeting meeting) {
            return LocationInfo.builder()
                    .placeName(meeting.getPlaceName())
                    .address(meeting.getAddress())
                    .zipCode(meeting.getZipCode())
                    .x(meeting.getX())
                    .y(meeting.getY())
                    .build();
        }
    }

    @Builder
    @Schema(description = "약속 등록자 정보")
    public record CreatedByInfo(
            @Schema(description = "MatchedMember ID", example = "100")
            Long matchedMemberId,
            @Schema(description = "User ID", example = "1")
            Long userId,
            @Schema(description = "닉네임", example = "bookii")
            String nickname,
            @Schema(description = "그룹 역할", example = "HOST")
            RoleStatus role
    ) {

        private static CreatedByInfo from(Meeting meeting) {
            return CreatedByInfo.builder()
                    .matchedMemberId(meeting.getCreatedBy().getId())
                    .userId(meeting.getCreatedBy().getUser().getId())
                    .nickname(meeting.getCreatedBy().getUser().getNickName())
                    .role(meeting.getCreatedBy().getRole())
                    .build();
        }
    }
}
