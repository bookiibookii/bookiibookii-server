package com.example.bookiibookii.domain.push.sender;

import com.example.bookiibookii.domain.push.config.FirebasePushProperties;
import com.example.bookiibookii.domain.push.dto.PushMessage;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Slf4j
@Component
@Profile({
        "dev & !local & !test",
        "prod & !local & !test",
        "v1 & !local & !test"
})
@RequiredArgsConstructor
public class FirebasePushSender implements PushSender {

    // todo : 변경 필요
    private static final String FIREBASE_APP_NAME = "bookiibookii-push";

    private final FirebasePushProperties properties;
    private final Environment environment;

    private FirebaseMessaging firebaseMessaging;

    @PostConstruct
    void initialize() {
        if (!properties.enabled()) {
            log.warn(
                    "Firebase push is disabled. activeProfiles={}, enabled=false, projectId={}",
                    activeProfiles(),
                    configuredProjectId()
            );
            return;
        }

        try {
            FirebaseOptions.Builder options = FirebaseOptions.builder()
                    .setCredentials(loadCredentials());
            if (StringUtils.hasText(properties.projectId())) {
                options.setProjectId(properties.projectId());
            }

            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(existing -> FIREBASE_APP_NAME.equals(existing.getName()))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options.build(), FIREBASE_APP_NAME));
            firebaseMessaging = FirebaseMessaging.getInstance(app);
            log.info(
                    "Firebase push sender initialized. activeProfiles={}, projectId={}, appName={}, initialized=true",
                    activeProfiles(),
                    effectiveProjectId(app),
                    app.getName()
            );
        } catch (Exception exception) {
            firebaseMessaging = null;
            log.error(
                    "Firebase push initialization failed; push delivery will be skipped. "
                            + "activeProfiles={}, enabled={}, projectId={}, credentialsPathConfigured={}, "
                            + "credentialsFileExists={}, credentialsFileReadable={}, exceptionClass={}, exceptionMessage={}",
                    activeProfiles(),
                    properties.enabled(),
                    configuredProjectId(),
                    credentialsPathConfigured(),
                    credentialsFileExists(),
                    credentialsFileReadable(),
                    exception.getClass().getName(),
                    safeLogMessage(exception.getMessage())
            );
        }
    }

    @Override
    public boolean isAvailable() {
        return firebaseMessaging != null;
    }

    @Override
    public void send(String deviceToken, PushMessage pushMessage) {
        if (firebaseMessaging == null) {
            return;
        }

        Message message = Message.builder()
                .setToken(deviceToken)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(pushMessage.title())
                        .setBody(pushMessage.body())
                        .build())
                .putAllData(pushMessage.data())
                .build();

        try {
            String messageId = firebaseMessaging.send(message);
            log.debug("FCM message sent. messageId={}", messageId);
        } catch (FirebaseMessagingException exception) {
            String safeMessage = safeExceptionMessage(exception.getMessage(), deviceToken);
            if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                throw new InvalidPushTokenException(safeMessage, exception);
            }
            throw new FcmPushDeliveryException(exception.getMessagingErrorCode(), safeMessage, exception);
        }
    }

    private String activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length == 0 ? "default" : Arrays.toString(profiles);
    }

    private String configuredProjectId() {
        return StringUtils.hasText(properties.projectId()) ? properties.projectId() : "<not-set>";
    }

    private String effectiveProjectId(FirebaseApp app) {
        String projectId = app.getOptions().getProjectId();
        return StringUtils.hasText(projectId) ? projectId : configuredProjectId();
    }

    private boolean credentialsPathConfigured() {
        return StringUtils.hasText(properties.credentialsPath());
    }

    private boolean credentialsFileExists() {
        try {
            return credentialsPathConfigured() && Files.exists(Path.of(properties.credentialsPath()));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private boolean credentialsFileReadable() {
        try {
            return credentialsPathConfigured() && Files.isReadable(Path.of(properties.credentialsPath()));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private String safeExceptionMessage(String message, String deviceToken) {
        if (!StringUtils.hasText(message)) {
            return "<no-message>";
        }
        if (!StringUtils.hasText(deviceToken)) {
            return message;
        }
        return safeLogMessage(message.replace(deviceToken, "[REDACTED_TOKEN]"));
    }

    private String safeLogMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "<no-message>";
        }
        String sanitized = message
                .replaceAll("(?s)-----BEGIN PRIVATE KEY-----.*?-----END PRIVATE KEY-----", "[REDACTED_PRIVATE_KEY]")
                .replace('\n', ' ')
                .replace('\r', ' ');
        return sanitized.length() <= 500 ? sanitized : sanitized.substring(0, 500) + "...";
    }

    private GoogleCredentials loadCredentials() throws IOException {
        if (!StringUtils.hasText(properties.credentialsPath())) {
            return GoogleCredentials.getApplicationDefault();
        }
        try (InputStream inputStream = new FileInputStream(properties.credentialsPath())) {
            return GoogleCredentials.fromStream(inputStream);
        }
    }
}
