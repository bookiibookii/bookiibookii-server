package com.example.bookiibookii.domain.location.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserDeliveryReqDTO {

    public record AddReqDTO(
            @NotBlank(message = "장소 이름은 필수입니다.")
            String placeName,

            @NotBlank(message = "주소는 필수입니다.")
            String address,

            @NotBlank(message = "우편번호는 필수입니다.")
            String zipCode,

            String addressDetail,

            @NotBlank(message = "배송지 등록을 위해 수취인 이름은 필수로 입력되어야 합니다.")
            String receiverName,

            @NotBlank(message = "배송지 등록을 위해 전화번호는 필수로 입력되어야 합니다.")
            @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
            String phone
    ) {}
}
