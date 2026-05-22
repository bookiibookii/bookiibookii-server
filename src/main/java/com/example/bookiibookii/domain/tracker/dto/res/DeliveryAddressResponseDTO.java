package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.tracker.entity.DeliveryAddress;
import lombok.Builder;

@Builder
public record DeliveryAddressResponseDTO(
        AddressDTO myAddress,
        AddressDTO partnerAddress,
        boolean canEditMyAddress
) {
    @Builder
    public record AddressDTO(
            String receiverName,
            String phoneNumber,
            String address,
            String addressDetail,
            String zipCode
    ) {
        public static AddressDTO from(DeliveryAddress snapshot) {
            return AddressDTO.builder()
                    .receiverName(snapshot.getReceiverName())
                    .phoneNumber(snapshot.getPhoneNumber())
                    .address(snapshot.getAddress())
                    .addressDetail(snapshot.getAddressDetail())
                    .zipCode(snapshot.getZipCode())
                    .build();
        }
    }
}
