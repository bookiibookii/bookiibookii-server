package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
@Schema(description = "직접 교환 약속 상세 응답")
public record MeetingResponseDTO(
        @Schema(description = "Meeting ID", example = "10")
        Long meetingId,
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
                .location(LocationInfo.from(meeting))
                .addressDetail(meeting.getAddressDetail())
                .scheduledAt(meeting.getScheduledAt())
                .createdBy(CreatedByInfo.from(meeting))
                .build();
    }

    @Builder
    @Schema(description = "약속 장소 정보")
    public record LocationInfo(
            @Schema(description = "Location ID", example = "1")
            Long locationId,
            @Schema(description = "장소명", example = "강남역")
            String placeName,
            @Schema(description = "주소", example = "서울특별시 강남구 강남대로 396")
            String address,
            @Schema(description = "우편번호", example = "06232")
            String zipCode
    ) {

        private static LocationInfo from(Meeting meeting) {
            return LocationInfo.builder()
                    .locationId(meeting.getLocation().getId())
                    .placeName(meeting.getLocation().getPlaceName())
                    .address(meeting.getLocation().getAddress())
                    .zipCode(meeting.getLocation().getZipCode())
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
