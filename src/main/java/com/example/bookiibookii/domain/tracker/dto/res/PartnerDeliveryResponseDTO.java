package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.enums.DeliveryCompany;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PartnerDeliveryResponseDTO(
        String deliveryId,
        DeliveryCompany deliveryCompany,
        String deliveryCompanyName,
        String trackingNumber,
        LocalDateTime registeredAt,
        boolean canConfirmReceived
) {
    public static PartnerDeliveryResponseDTO of(Delivery delivery, boolean canConfirmReceived) {
        return PartnerDeliveryResponseDTO.builder()
                .deliveryId(delivery.getId())
                .deliveryCompany(delivery.getDeliveryCompany())
                .deliveryCompanyName(delivery.getDeliveryCompany().getDisplayName())
                .trackingNumber(delivery.getTrackingNumber())
                .registeredAt(delivery.getCreatedAt())
                .canConfirmReceived(canConfirmReceived)
                .build();
    }
}
