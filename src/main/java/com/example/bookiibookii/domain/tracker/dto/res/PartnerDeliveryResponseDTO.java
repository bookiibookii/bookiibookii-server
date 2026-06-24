package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.enums.DeliveryCompany;
import lombok.Builder;

import java.time.Instant;

@Builder
public record PartnerDeliveryResponseDTO(
        String deliveryId,
        DeliveryCompany deliveryCompany,
        String deliveryCompanyName,
        String trackingNumber,
        Instant trackingRegisteredAt,
        boolean canConfirmReceived
) {
    public static PartnerDeliveryResponseDTO of(Delivery delivery, boolean canConfirmReceived) {
        return PartnerDeliveryResponseDTO.builder()
                .deliveryId(delivery.getId())
                .deliveryCompany(delivery.getDeliveryCompany())
                .deliveryCompanyName(delivery.getDeliveryCompany().getDisplayName())
                .trackingNumber(delivery.getTrackingNumber())
                .trackingRegisteredAt(delivery.getTrackingRegisteredAt())
                .canConfirmReceived(canConfirmReceived)
                .build();
    }
}
