package com.example.bookiibookii.domain.tracker.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeliveryTest {

    @Test
    void confirmReceivedStoresOnlyReceivedConfirmedAt() {
        Instant deliveredAt = Instant.parse("2026-06-19T05:00:00Z");
        Instant confirmedAt = Instant.parse("2026-06-20T05:00:00Z");
        Delivery delivery = Delivery.builder()
                .deliveredAt(deliveredAt)
                .build();

        delivery.confirmReceived(confirmedAt);

        assertThat(delivery.getReceivedConfirmedAt()).isEqualTo(confirmedAt);
        assertThat(delivery.getDeliveredAt()).isEqualTo(deliveredAt);
    }

    @Test
    void confirmReceivedRejectsNullConfirmedAtBeforeChangingState() {
        Delivery delivery = Delivery.builder().build();

        assertThatThrownBy(() -> delivery.confirmReceived(null))
                .isInstanceOf(NullPointerException.class);
        assertThat(delivery.getReceivedConfirmedAt()).isNull();
    }
}
