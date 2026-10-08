package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.PushMessage;
import com.example.bookiibookii.domain.push.entity.DeviceToken;
import com.example.bookiibookii.domain.push.repository.DeviceTokenRepository;
import com.example.bookiibookii.domain.push.sender.PushSender;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(PushServiceTransactionBoundaryTest.Config.class)
class PushServiceTransactionBoundaryTest {

    @Autowired
    private PushService pushService;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private RecordingPushSender pushSender;

    @Test
    void sendsPushAfterTokenLookupTransactionHasEnded() {
        AtomicBoolean transactionActiveDuringLookup = new AtomicBoolean();
        when(deviceTokenRepository.findAllByUserIdAndActiveTrue(7L)).thenAnswer(invocation -> {
            transactionActiveDuringLookup.set(TransactionSynchronizationManager.isActualTransactionActive());
            return List.of(DeviceToken.builder().id(1L).token("token").active(true).build());
        });

        pushService.sendToUser(7L, new PushMessage("title", "body", Map.of()));

        assertThat(transactionActiveDuringLookup).isTrue();
        assertThat(pushSender.transactionActiveDuringSend()).isFalse();
    }

    @Configuration
    @EnableTransactionManagement
    @Import({
            PushService.class,
            DeviceTokenQueryService.class,
            DeviceTokenService.class,
            DeviceTokenRegistrationExecutor.class
    })
    static class Config {

        @Bean
        DeviceTokenRepository deviceTokenRepository() {
            return mock(DeviceTokenRepository.class);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        RecordingPushSender pushSender() {
            return new RecordingPushSender();
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }

        @Bean
        PlatformTransactionManager transactionManager() {
            return new TestTransactionManager();
        }
    }

    static class RecordingPushSender implements PushSender {

        private boolean transactionActiveDuringSend;

        @Override
        public void send(String deviceToken, PushMessage message) {
            transactionActiveDuringSend = TransactionSynchronizationManager.isActualTransactionActive();
        }

        boolean transactionActiveDuringSend() {
            return transactionActiveDuringSend;
        }
    }

    static class TestTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
