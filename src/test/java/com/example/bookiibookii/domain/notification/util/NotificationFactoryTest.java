package com.example.bookiibookii.domain.notification.util;

import com.example.bookiibookii.domain.notification.dto.NotificationPayload;
import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.notification.enums.RedirectType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class NotificationFactoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final NotificationFactory notificationFactory =
            new NotificationFactory(mock(UserRepository.class), objectMapper);

    @Test
    void serializesTypedPayloadAndOmitsUnusedFields() throws Exception {
        NotificationPayload payload = NotificationPayload.builder()
                .redirectType(RedirectType.TRACKER_DETAIL)
                .groupId(1L)
                .exchangeType(ExchangeType.DELIVERY)
                .exchangeRound(ExchangeRound.FIRST_EXCHANGE)
                .build();

        JsonNode json = objectMapper.readTree(notificationFactory.toJson(payload));

        assertThat(json.get("redirectType").asText()).isEqualTo("TRACKER_DETAIL");
        assertThat(json.get("groupId").asLong()).isEqualTo(1L);
        assertThat(json.get("exchangeType").asText()).isEqualTo("DELIVERY");
        assertThat(json.get("exchangeRound").asText()).isEqualTo("FIRST_EXCHANGE");
        assertThat(json.has("commentId")).isFalse();
        assertThat(json.has("deliveryId")).isFalse();
        assertThat(json.has("memberBookId")).isFalse();
    }

    @Test
    void serializesMemberBookIdForBookCardDetailPayload() throws Exception {
        NotificationPayload payload = NotificationPayload.builder()
                .redirectType(RedirectType.BOOK_CARD_DETAIL)
                .groupId(10L)
                .memberBookId(15L)
                .cardId(20L)
                .build();

        JsonNode json = objectMapper.readTree(notificationFactory.toJson(payload));

        assertThat(json.get("redirectType").asText()).isEqualTo("BOOK_CARD_DETAIL");
        assertThat(json.get("groupId").asLong()).isEqualTo(10L);
        assertThat(json.get("memberBookId").asLong()).isEqualTo(15L);
        assertThat(json.get("cardId").asLong()).isEqualTo(20L);
    }
}
