package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.DeviceTokenRequest;
import com.example.bookiibookii.domain.push.entity.DeviceToken;
import com.example.bookiibookii.domain.push.repository.DeviceTokenRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DeviceTokenRegistrationExecutor {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void register(Long userId, DeviceTokenRequest.Register request) {
        User user = userRepository.getReferenceById(userId);
        Instant now = clock.instant();

        DeviceToken deviceToken = deviceTokenRepository.findByToken(request.token())
                .map(existing -> {
                    existing.refresh(user, request.platform(), now);
                    return existing;
                })
                .orElseGet(() -> DeviceToken.register(user, request.token(), request.platform(), now));

        deviceTokenRepository.saveAndFlush(deviceToken);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean refreshExisting(Long userId, DeviceTokenRequest.Register request) {
        return deviceTokenRepository.findByToken(request.token())
                .map(existing -> {
                    User user = userRepository.getReferenceById(userId);
                    existing.refresh(user, request.platform(), clock.instant());
                    deviceTokenRepository.saveAndFlush(existing);
                    return true;
                })
                .orElse(false);
    }
}
