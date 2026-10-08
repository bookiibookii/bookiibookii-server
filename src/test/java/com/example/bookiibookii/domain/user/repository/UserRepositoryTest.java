package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.enums.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void withdrawUserUpdatesStatusAndInstantTimestamp() {
        User user = userRepository.saveAndFlush(User.builder()
                .socialType(SocialType.KAKAO)
                .socialId("withdraw-user")
                .build());
        Instant withdrawnAt = Instant.parse("2030-01-01T00:00:00Z");

        userRepository.withdrawUser(user.getId(), withdrawnAt);

        User withdrawnUser = userRepository.findByIdIncludingWithdrawn(user.getId()).orElseThrow();
        assertThat(withdrawnUser.getStatus()).isEqualTo(Status.WITHDRAWN);
        assertThat(withdrawnUser.getUpdatedAt()).isEqualTo(withdrawnAt);
    }
}
