package com.example.bookiibookii.domain.location.dto.res;

import com.example.bookiibookii.domain.location.entity.UserDelivery;

public class UserDeliveryResDTO {

    public record UserDeliveryDto(
            Long Id,
            String placeName,
            String address,
            String zipCode,
            String addressDetail,
            String receiverName,
            String phone,
            boolean isDefault
    ) {
        public static UserDeliveryDto from(UserDelivery ud) {
            return new UserDeliveryDto(
                    ud.getId(),
                    ud.getLocation().getPlaceName(),
                    ud.getLocation().getAddress(),
                    ud.getLocation().getZipCode(),
                    ud.getAddressDetail(),
                    ud.getReceiverName(),
                    ud.getPhone(),
                    ud.isDefault()
            );
        }
    }
}
