package com.example.bookiibookii.domain.location.dto.res;

import com.example.bookiibookii.domain.location.entity.UserExchange;

public class UserExchangeResDTO {

    public record UserExchangeDto(
            Long Id,
            String placeName,
            String address,
            String zipCode,
            String addressDetail,
            boolean isDefault
    ) {
        public static UserExchangeDto from(UserExchange ue) {
            return new UserExchangeDto(
                    ue.getId(),
                    ue.getLocation().getPlaceName(),
                    ue.getLocation().getAddress(),
                    ue.getLocation().getZipCode(),
                    ue.getAddressDetail(),
                    ue.isDefault()
            );
        }
    }
}
