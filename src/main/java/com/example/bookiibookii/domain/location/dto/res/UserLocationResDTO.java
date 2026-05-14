package com.example.bookiibookii.domain.location.dto.res;

import com.example.bookiibookii.domain.location.entity.UserLocation;
import com.example.bookiibookii.domain.location.enums.LocationType;

public class UserLocationResDTO {

    public record UserLocationDto(
            Long userLocationId,
            LocationType type,
            String placeName,
            String address,
            String zipCode,
            String addressDetail,
            String receiverName,
            String phone
    ) {
        public static UserLocationDto from(UserLocation ul) {
            return new UserLocationDto(
                    ul.getUserLocationId(),
                    ul.getType(),
                    ul.getLocation().getPlaceName(),
                    ul.getLocation().getAddress(),
                    ul.getLocation().getZipCode(),
                    ul.getAddressDetail(),
                    ul.getReceiverName(),
                    ul.getPhone()
            );
        }
    }
}
