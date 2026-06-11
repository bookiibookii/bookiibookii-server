package com.example.bookiibookii.domain.push.service;

import com.example.bookiibookii.domain.push.dto.ActiveDeviceToken;
import com.example.bookiibookii.domain.push.entity.DeviceToken;
import com.example.bookiibookii.domain.push.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceTokenQueryService {

    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional(readOnly = true)
    public List<ActiveDeviceToken> findActiveTokens(Long userId) {
        return deviceTokenRepository.findAllByUserIdAndActiveTrue(userId).stream()
                .map(this::toActiveDeviceToken)
                .toList();
    }

    private ActiveDeviceToken toActiveDeviceToken(DeviceToken deviceToken) {
        return new ActiveDeviceToken(deviceToken.getId(), deviceToken.getToken());
    }
}
