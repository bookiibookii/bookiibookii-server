package com.example.bookiibookii.domain.push.sender;

import com.example.bookiibookii.domain.push.config.FirebasePushProperties;
import com.example.bookiibookii.domain.push.dto.PushMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class FirebasePushSenderTest {

    @Test
    void disabledFirebaseRemainsUnavailableAndSendIsSafeNoOp() {
        FirebasePushSender sender = new FirebasePushSender(
                new FirebasePushProperties(false, "", "bkbk-dev"),
                new MockEnvironment().withProperty("spring.profiles.active", "v1")
        );

        assertThatCode(sender::initialize).doesNotThrowAnyException();
        assertThat(sender.isAvailable()).isFalse();
        assertThatCode(() -> sender.send(
                "sensitive-device-token",
                new PushMessage("title", "body", Map.of())
        )).doesNotThrowAnyException();
    }
}
