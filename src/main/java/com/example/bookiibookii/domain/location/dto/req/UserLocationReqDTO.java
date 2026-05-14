package com.example.bookiibookii.domain.location.dto.req;

import com.example.bookiibookii.domain.location.enums.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UserLocationReqDTO {

    public record AddReqDTO(
            @NotNull(message = "장소 타입은 필수입니다.")
            LocationType type,

            @NotBlank(message = "장소 이름은 필수입니다.")
            String placeName,

            @NotBlank(message = "주소는 필수입니다.")
            String address,

            @NotBlank(message = "우편번호는 필수입니다.")
            String zipCode,

            String addressDetail,

            // DELIVERY 전용 — 서비스 레이어에서 필수값 검증
            String receiverName,

            @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
            String phone
    ) {}
}
