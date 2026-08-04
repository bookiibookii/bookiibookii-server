package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.PushMessage;
import com.example.bookiibookii.domain.push.dto.ActiveDeviceToken;
import com.example.bookiibookii.domain.push.enums.DevicePlatform;
import com.example.bookiibookii.domain.push.sender.FcmPushDeliveryException;
import com.example.bookiibookii.domain.push.sender.InvalidPushTokenException;
import com.example.bookiibookii.domain.push.sender.PushSender;
import com.google.firebase.messaging.MessagingErrorCode;
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
        String notificationId = message.data().getOrDefault("notificationId", "<unknown>");
        String notificationType = message.data().getOrDefault("type", "<unknown>");
        long androidCount = countPlatform(tokens, DevicePlatform.ANDROID);
        long iosCount = countPlatform(tokens, DevicePlatform.IOS);

        if (!pushSender.isAvailable()) {
            log.warn(
                    "Push delivery skipped because sender is unavailable. notificationId={}, userId={}, "
                            + "notificationType={}, activeTokenCount={}, androidTokenCount={}, iosTokenCount={}",
                    notificationId, userId, notificationType, tokens.size(), androidCount, iosCount
            );
            return;
        }

        int successCount = 0;
        int failureCount = 0;
        int deactivatedCount = 0;
        for (ActiveDeviceToken token : tokens) {
            try {
                pushSender.send(token.token(), message);
                successCount++;
            } catch (InvalidPushTokenException exception) {
                failureCount++;
                deviceTokenService.deactivateInvalidToken(token.token());
                deactivatedCount++;
                log.warn(
                        "FCM token rejected. errorCode=UNREGISTERED, exceptionClass={}, exceptionMessage={}, "
                                + "userId={}, platform={}, tokenId={}",
                        causeClass(exception), safeExceptionMessage(exception.getMessage(), token.token()),
                        userId, token.platform(),
                        maskedToken(token.token())
                );
            } catch (FcmPushDeliveryException exception) {
                failureCount++;
                logFcmFailure(userId, token, exception);
            } catch (RuntimeException exception) {
                failureCount++;
                log.warn(
                        "Push delivery failed. errorCode=UNKNOWN, exceptionClass={}, exceptionMessage={}, "
                                + "userId={}, platform={}, tokenId={}",
                        exception.getClass().getName(), safeExceptionMessage(exception.getMessage(), token.token()),
                        userId, token.platform(),
                        maskedToken(token.token())
                );
            }
        }
        log.info(
                "Push delivery summary. notificationId={}, userId={}, notificationType={}, activeTokenCount={}, "
                        + "androidTokenCount={}, iosTokenCount={}, successCount={}, failureCount={}, deactivatedCount={}",
                notificationId, userId, notificationType, tokens.size(), androidCount, iosCount,
                successCount, failureCount, deactivatedCount
        );
    }

    private long countPlatform(List<ActiveDeviceToken> tokens, DevicePlatform platform) {
        return tokens.stream().filter(token -> token.platform() == platform).count();
    }

    private void logFcmFailure(Long userId, ActiveDeviceToken token, FcmPushDeliveryException exception) {
        MessagingErrorCode errorCode = exception.getErrorCode();
        String format = "FCM delivery failed. errorCode={}, exceptionClass={}, exceptionMessage={}, "
                + "userId={}, platform={}, tokenId={}";
        if (errorCode == MessagingErrorCode.SENDER_ID_MISMATCH
                || errorCode == MessagingErrorCode.THIRD_PARTY_AUTH_ERROR) {
            log.error(format, errorCode, causeClass(exception),
                    safeExceptionMessage(exception.getMessage(), token.token()), userId,
                    token.platform(), maskedToken(token.token()));
            return;
        }
        log.warn(format, errorCode, causeClass(exception),
                safeExceptionMessage(exception.getMessage(), token.token()), userId,
                token.platform(), maskedToken(token.token()));
    }

    private String safeExceptionMessage(String message, String token) {
        if (message == null) {
            return "<no-message>";
        }
        return token == null || token.isBlank() ? message : message.replace(token, "[REDACTED_TOKEN]");
    }

    private String causeClass(RuntimeException exception) {
        return exception.getCause() == null
                ? exception.getClass().getName()
                : exception.getCause().getClass().getName();
    }

    private String maskedToken(String token) {
        if (token == null || token.length() < 9) {
            return "********";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
