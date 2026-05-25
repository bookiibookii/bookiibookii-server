package com.example.bookiibookii.domain.location.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class UserExchangeReqDTO {

    @Schema(name = "UserExchangeAddReqDTO")
    public record AddReqDTO(
            @NotBlank(message = "장소 이름은 필수입니다.")
            String placeName,

            @NotBlank(message = "주소는 필수입니다.")
            String address,

            @NotBlank(message = "우편번호는 필수입니다.")
            String zipCode,

            String addressDetail
    ) {}
}
