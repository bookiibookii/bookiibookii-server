package com.example.bookiibookii.domain.location.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserDeliveryReqDTO {

    @Schema(name = "UserDeliveryAddReqDTO")
    public record AddReqDTO(
            @JsonProperty("placeName")
            @Schema(description = "장소 이름")
            @NotBlank(message = "장소 이름은 필수입니다.")
            String placeName,

            @JsonProperty("address")
            @Schema(description = "주소")
            @NotBlank(message = "주소는 필수입니다.")
            String address,

            @JsonProperty("zipCode")
            @Schema(description = "우편번호")
            @NotBlank(message = "우편번호는 필수입니다.")
            String zipCode,

            @JsonProperty("addressDetail")
            @Schema(description = "상세 주소")
            String addressDetail,

            @JsonProperty("receiverName")
            @Schema(description = "수취인 이름")
            @NotBlank(message = "배송지 등록을 위해 수취인 이름은 필수로 입력되어야 합니다.")
            String receiverName,

            @JsonProperty("phone")
            @Schema(description = "전화번호", example = "010-1234-5678")
            @NotBlank(message = "배송지 등록을 위해 전화번호는 필수로 입력되어야 합니다.")
            @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
            String phone
    ) {}
}
