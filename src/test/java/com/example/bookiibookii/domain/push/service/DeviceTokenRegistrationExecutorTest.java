package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.DeviceTokenRequest;
import com.example.bookiibookii.domain.push.entity.DeviceToken;
import com.example.bookiibookii.domain.push.enums.DevicePlatform;
import com.example.bookiibookii.domain.push.repository.DeviceTokenRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTokenRegistrationExecutorTest {

    private static final Instant NOW = Instant.parse("2026-06-20T05:00:00Z");

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private UserRepository userRepository;

    private DeviceTokenRegistrationExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new DeviceTokenRegistrationExecutor(
                deviceTokenRepository,
                userRepository,
                Clock.fixed(NOW, ZoneId.of("UTC"))
        );
    }

    @Test
    void newTokenCreatesDeviceToken() {
        User user = User.builder().id(1L).build();
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(deviceTokenRepository.findByToken("new-token")).thenReturn(Optional.empty());

        executor.register(1L, request("new-token"));

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).saveAndFlush(captor.capture());
        DeviceToken saved = captor.getValue();
        assertThat(saved.getId()).isNull();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getToken()).isEqualTo("new-token");
        assertThat(saved.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getLastUsedAt()).isNotNull();
    }

    @Test
    void sameUserReregistrationRefreshesExistingRow() {
        User user = User.builder().id(1L).build();
        Instant previousUsedAt = NOW.minusSeconds(24 * 60 * 60);
        DeviceToken existing = token(10L, user, false, previousUsedAt);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(deviceTokenRepository.findByToken("same-token")).thenReturn(Optional.of(existing));

        executor.register(1L, request("same-token"));

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).saveAndFlush(captor.capture());
        DeviceToken saved = captor.getValue();
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getPlatform()).isEqualTo(DevicePlatform.ANDROID);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getLastUsedAt()).isAfter(previousUsedAt);
    }

    @Test
    void differentUserReregistrationReassignsExistingRow() {
        User oldUser = User.builder().id(1L).build();
        User currentUser = User.builder().id(2L).build();
        DeviceToken existing = token(10L, oldUser, true, NOW.minusSeconds(24 * 60 * 60));
        when(userRepository.getReferenceById(2L)).thenReturn(currentUser);
        when(deviceTokenRepository.findByToken("same-token")).thenReturn(Optional.of(existing));

        executor.register(2L, request("same-token"));

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(captor.getValue().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getUser()).isSameAs(currentUser);
    }

    @Test
    void refreshExistingReturnsFalseWhenRaceWinnerIsNotVisible() {
        when(deviceTokenRepository.findByToken("same-token")).thenReturn(Optional.empty());

        boolean refreshed = executor.refreshExisting(2L, request("same-token"));

        assertThat(refreshed).isFalse();
        verify(userRepository, never()).getReferenceById(2L);
    }

    private DeviceTokenRequest.Register request(String token) {
        return new DeviceTokenRequest.Register(token, DevicePlatform.ANDROID);
    }

    private DeviceToken token(Long id, User user, boolean active, Instant lastUsedAt) {
        return DeviceToken.builder()
                .id(id)
                .user(user)
                .token("same-token")
                .platform(DevicePlatform.ANDROID)
                .active(active)
                .lastUsedAt(lastUsedAt)
                .build();
    }
}
