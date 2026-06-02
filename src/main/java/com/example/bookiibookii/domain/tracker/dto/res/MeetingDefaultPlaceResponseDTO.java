package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.group.entity.GroupPlace;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@Schema(description = "직접 교환 기본 장소 응답")
public record MeetingDefaultPlaceResponseDTO(
        @Schema(description = "장소명", example = "스타벅스 강남점")
        String placeName,

        @Schema(description = "주소", example = "서울시 강남구 강남대로 100")
        String address,

        @Schema(description = "우편번호", example = "12345")
        String zipCode,

        @Schema(description = "X 좌표(경도)", example = "127.027621")
        BigDecimal x,

        @Schema(description = "Y 좌표(위도)", example = "37.497942")
        BigDecimal y,

        @Schema(description = "상세 주소", example = "2층")
        String addressDetail
) {
    public static MeetingDefaultPlaceResponseDTO from(GroupPlace groupPlace) {
        return MeetingDefaultPlaceResponseDTO.builder()
                .placeName(groupPlace.getPlaceName())
                .address(groupPlace.getAddress())
                .zipCode(groupPlace.getZipCode())
                .x(groupPlace.getX())
                .y(groupPlace.getY())
                .addressDetail(groupPlace.getAddressDetail())
                .build();
    }
}
