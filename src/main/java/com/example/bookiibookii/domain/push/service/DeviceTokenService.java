package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.DeviceTokenRequest;
import com.example.bookiibookii.domain.push.entity.DeviceToken;
import com.example.bookiibookii.domain.push.repository.DeviceTokenRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void register(Long userId, DeviceTokenRequest.Register request) {
        User user = userRepository.getReferenceById(userId);
        LocalDateTime now = LocalDateTime.now();

        DeviceToken deviceToken = deviceTokenRepository.findByToken(request.token())
                .map(existing -> {
                    existing.refresh(user, request.platform(), now);
                    return existing;
                })
                .orElseGet(() -> DeviceToken.register(user, request.token(), request.platform(), now));

        deviceTokenRepository.save(deviceToken);
    }

    @Transactional
    public void deactivate(Long userId, String token) {
        deviceTokenRepository.findByTokenAndUserId(token, userId)
                .ifPresent(DeviceToken::deactivate);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deactivateInvalidToken(String token) {
        deviceTokenRepository.findByToken(token)
                .ifPresent(DeviceToken::deactivate);
    }
}
