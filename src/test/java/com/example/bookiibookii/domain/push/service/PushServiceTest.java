package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.ActiveDeviceToken;
import com.example.bookiibookii.domain.push.dto.PushMessage;
import com.example.bookiibookii.domain.push.enums.DevicePlatform;
import com.example.bookiibookii.domain.push.sender.InvalidPushTokenException;
import com.example.bookiibookii.domain.push.sender.PushSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushServiceTest {

    @Mock
    private DeviceTokenQueryService deviceTokenQueryService;

    @Mock
    private DeviceTokenService deviceTokenService;

    @Mock
    private PushSender pushSender;

    private PushService pushService;

    @BeforeEach
    void setUp() {
        pushService = new PushService(deviceTokenQueryService, deviceTokenService, pushSender);
        lenient().when(pushSender.isAvailable()).thenReturn(true);
    }

    @Test
    void sendsToAllActiveTokensEvenWhenOneDeliveryFails() {
        PushMessage message = new PushMessage("title", "body", Map.of("groupId", "1"));
        when(deviceTokenQueryService.findActiveTokens(7L))
                .thenReturn(List.of(token(1L, "first", DevicePlatform.ANDROID),
                        token(2L, "second", DevicePlatform.IOS)));
        doThrow(new IllegalStateException("FCM unavailable"))
                .when(pushSender).send("first", message);

        pushService.sendToUser(7L, message);

        InOrder inOrder = inOrder(pushSender);
        inOrder.verify(pushSender).send("first", message);
        inOrder.verify(pushSender).send("second", message);
    }

    @Test
    void deactivatesUnregisteredTokenAndContinues() {
        PushMessage message = new PushMessage("title", "body", Map.of());
        when(deviceTokenQueryService.findActiveTokens(7L))
                .thenReturn(List.of(token(1L, "invalid", DevicePlatform.IOS),
                        token(2L, "valid", DevicePlatform.ANDROID)));
        doThrow(new InvalidPushTokenException("unregistered", new RuntimeException()))
                .when(pushSender).send("invalid", message);

        pushService.sendToUser(7L, message);

        verify(deviceTokenService).deactivateInvalidToken("invalid");
        verify(pushSender).send("valid", message);
    }

    @Test
    void unavailableSenderSkipsDeliveryWithoutThrowing() {
        PushMessage message = new PushMessage("title", "body", Map.of("notificationId", "100"));
        when(deviceTokenQueryService.findActiveTokens(7L))
                .thenReturn(List.of(token(1L, "ios-token", DevicePlatform.IOS)));
        when(pushSender.isAvailable()).thenReturn(false);

        pushService.sendToUser(7L, message);

        verify(pushSender, never()).send("ios-token", message);
        verify(deviceTokenService, never()).deactivateInvalidToken("ios-token");
    }

    private ActiveDeviceToken token(Long id, String value, DevicePlatform platform) {
        return new ActiveDeviceToken(id, value, platform);
    }
}
