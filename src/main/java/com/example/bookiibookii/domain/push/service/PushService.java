package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.PushMessage;
import com.example.bookiibookii.domain.push.dto.ActiveDeviceToken;
import com.example.bookiibookii.domain.push.sender.InvalidPushTokenException;
import com.example.bookiibookii.domain.push.sender.PushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final DeviceTokenQueryService deviceTokenQueryService;
    private final DeviceTokenService deviceTokenService;
    private final PushSender pushSender;

    public void sendToUser(Long userId, PushMessage message) {
        List<ActiveDeviceToken> tokens = deviceTokenQueryService.findActiveTokens(userId);
        for (ActiveDeviceToken token : tokens) {
            try {
                pushSender.send(token.token(), message);
            } catch (InvalidPushTokenException exception) {
                log.warn("Invalid push token detected. deviceTokenId={}, userId={}", token.id(), userId);
                deviceTokenService.deactivateInvalidToken(token.token());
            } catch (RuntimeException exception) {
                log.warn(
                        "Push delivery failed. deviceTokenId={}, userId={}",
                        token.id(),
                        userId,
                        exception
                );
            }
        }
    }
}