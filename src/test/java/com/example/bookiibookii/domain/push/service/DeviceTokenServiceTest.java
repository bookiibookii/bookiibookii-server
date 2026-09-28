package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.DeviceTokenRequest;
import com.example.bookiibookii.domain.push.entity.DeviceToken;
import com.example.bookiibookii.domain.push.enums.DevicePlatform;
import com.example.bookiibookii.domain.push.repository.DeviceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private DeviceTokenRegistrationExecutor registrationExecutor;

    private DeviceTokenService deviceTokenService;

    @BeforeEach
    void setUp() {
        deviceTokenService = new DeviceTokenService(deviceTokenRepository, registrationExecutor);
    }

    @Test
    void duplicateInsertRaceRefreshesExistingTokenWithoutFailing() {
        DeviceTokenRequest.Register request =
                new DeviceTokenRequest.Register("same-token", DevicePlatform.ANDROID);
        doThrow(new DataIntegrityViolationException("duplicate token"))
                .when(registrationExecutor).register(2L, request);
        when(registrationExecutor.refreshExisting(2L, request)).thenReturn(true);

        assertThatCode(() -> deviceTokenService.register(2L, request))
                .doesNotThrowAnyException();

        verify(registrationExecutor).refreshExisting(2L, request);
    }

    @Test
    void deactivateOnlyLooksUpTokenOwnedByCurrentUser() {
        DeviceToken token = DeviceToken.builder()
                .token("token")
                .active(true)
                .build();
        when(deviceTokenRepository.findByTokenAndUserId("token", 3L)).thenReturn(Optional.of(token));

        deviceTokenService.deactivate(3L, "token");

        assertThat(token.isActive()).isFalse();
    }
}
