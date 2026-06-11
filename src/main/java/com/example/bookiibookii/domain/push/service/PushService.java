package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.PushMessage;
import com.example.bookiibookii.domain.push.entity.DeviceToken;
import com.example.bookiibookii.domain.push.repository.DeviceTokenRepository;
import com.example.bookiibookii.domain.push.sender.InvalidPushTokenException;
import com.example.bookiibookii.domain.push.sender.PushSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenService deviceTokenService;
    private final PushSender pushSender;

    @Transactional(readOnly = true)
    public void sendToUser(Long userId, PushMessage message) {
        List<DeviceToken> tokens = deviceTokenRepository.findAllByUserIdAndActiveTrue(userId);
        for (DeviceToken token : tokens) {
            try {
                pushSender.send(token.getToken(), message);
            } catch (InvalidPushTokenException exception) {
                log.warn("Invalid push token detected. deviceTokenId={}, userId={}", token.getId(), userId);
                deviceTokenService.deactivateInvalidToken(token.getToken());
            } catch (RuntimeException exception) {
                log.warn(
                        "Push delivery failed. deviceTokenId={}, userId={}",
                        token.getId(),
                        userId,
                        exception
                );
            }
        }
    }
}