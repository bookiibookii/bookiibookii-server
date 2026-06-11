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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

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

    private FirebaseMessaging firebaseMessaging;

    @PostConstruct
    void initialize() {
        if (!properties.enabled()) {
            log.warn("Firebase push is disabled. Set push.firebase.enabled=true to enable FCM delivery.");
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
            log.info("Firebase push sender initialized.");
        } catch (Exception exception) {
            firebaseMessaging = null;
            log.error("Firebase push initialization failed. Push delivery will be skipped.", exception);
        }
    }

    @Override
    public void send(String deviceToken, PushMessage pushMessage) {
        if (firebaseMessaging == null) {
            log.warn("Firebase push skipped because the sender is not initialized.");
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
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException exception) {
            if (exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                throw new InvalidPushTokenException("FCM token is unregistered", exception);
            }
            throw new IllegalStateException("FCM send failed", exception);
        }
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
