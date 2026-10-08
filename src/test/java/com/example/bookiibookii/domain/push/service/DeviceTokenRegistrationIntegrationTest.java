package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.DeviceTokenRequest;
import com.example.bookiibookii.domain.push.entity.DeviceToken;
import com.example.bookiibookii.domain.push.enums.DevicePlatform;
import com.example.bookiibookii.domain.push.repository.DeviceTokenRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.global.time.TimeConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DeviceTokenService.class, DeviceTokenRegistrationExecutor.class, TimeConfig.class})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DeviceTokenRegistrationIntegrationTest {

    @Autowired
    private DeviceTokenService deviceTokenService;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        deviceTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void newTokenCreatesDeviceToken() {
        User user = saveUser("new-token-user");

        deviceTokenService.register(user.getId(), request("new-token"));

        assertThat(deviceTokenRepository.count()).isEqualTo(1);
        DeviceToken saved = findToken("new-token");
        assertThat(saved.getUser().getId()).isEqualTo(user.getId());
        assertThat(saved.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getLastUsedAt()).isNotNull();
    }

    @Test
    void sameUserReregistrationUpdatesExistingRowWithoutIncreasingCount() throws InterruptedException {
        User user = saveUser("same-user");
        deviceTokenService.register(user.getId(), request("same-token"));
        DeviceToken first = findToken("same-token");
        Long deviceTokenId = first.getId();
        Instant firstLastUsedAt = first.getLastUsedAt();
        Instant firstUpdatedAt = first.getUpdatedAt();
        Thread.sleep(10);

        deviceTokenService.register(user.getId(), request("same-token"));

        assertThat(deviceTokenRepository.count()).isEqualTo(1);
        DeviceToken refreshed = findToken("same-token");
        assertThat(refreshed.getId()).isEqualTo(deviceTokenId);
        assertThat(refreshed.getUser().getId()).isEqualTo(user.getId());
        assertThat(refreshed.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(refreshed.isActive()).isTrue();
        assertThat(refreshed.getLastUsedAt()).isAfter(firstLastUsedAt);
        assertThat(refreshed.getUpdatedAt()).isAfter(firstUpdatedAt);
    }

    @Test
    void differentUserReregistrationReassignsExistingRow() {
        User oldUser = saveUser("old-user");
        User currentUser = saveUser("current-user");
        deviceTokenService.register(oldUser.getId(), request("shared-token"));
        Long deviceTokenId = findToken("shared-token").getId();

        deviceTokenService.register(currentUser.getId(), request("shared-token"));

        assertThat(deviceTokenRepository.count()).isEqualTo(1);
        DeviceToken reassigned = findToken("shared-token");
        assertThat(reassigned.getId()).isEqualTo(deviceTokenId);
        assertThat(reassigned.getUser().getId()).isEqualTo(currentUser.getId());
        assertThat(reassigned.isActive()).isTrue();
    }

    private User saveUser(String socialId) {
        return userRepository.saveAndFlush(
                User.builder()
                        .socialType(SocialType.KAKAO)
                        .socialId(socialId)
                        .build()
        );
    }

    private DeviceTokenRequest.Register request(String token) {
        return new DeviceTokenRequest.Register(token, DevicePlatform.ANDROID);
    }

    private DeviceToken findToken(String token) {
        return deviceTokenRepository.findByToken(token).orElseThrow();
    }
}
