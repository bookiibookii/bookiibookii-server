package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.DeviceTokenRequest;
import com.example.bookiibookii.domain.push.entity.DeviceToken;
import com.example.bookiibookii.domain.push.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final DeviceTokenRegistrationExecutor registrationExecutor;

    public void register(Long userId, DeviceTokenRequest.Register request) {
        try {
            registrationExecutor.register(userId, request);
        } catch (DataIntegrityViolationException exception) {
            if (!registrationExecutor.refreshExisting(userId, request)) {
                throw exception;
            }
        }
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
