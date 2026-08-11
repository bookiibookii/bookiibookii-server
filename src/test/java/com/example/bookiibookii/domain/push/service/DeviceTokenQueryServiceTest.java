package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.ActiveDeviceToken;
import com.example.bookiibookii.domain.push.entity.DeviceToken;
import com.example.bookiibookii.domain.push.enums.DevicePlatform;
import com.example.bookiibookii.domain.push.repository.DeviceTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceTokenQueryServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Test
    void returnsDetachedValuesNeededForPushDelivery() {
        DeviceToken deviceToken = DeviceToken.builder()
                .id(1L)
                .token("token")
                .platform(DevicePlatform.IOS)
                .active(true)
                .build();
        when(deviceTokenRepository.findAllByUserIdAndActiveTrue(7L))
                .thenReturn(List.of(deviceToken));
        DeviceTokenQueryService service = new DeviceTokenQueryService(deviceTokenRepository);

        List<ActiveDeviceToken> result = service.findActiveTokens(7L);

        assertThat(result).containsExactly(new ActiveDeviceToken(1L, "token", DevicePlatform.IOS));
    }
}
