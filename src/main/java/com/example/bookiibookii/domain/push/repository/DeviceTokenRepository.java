package com.example.bookiibookii.domain.push.repository;

import com.example.bookiibookii.domain.push.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    Optional<DeviceToken> findByTokenAndUserId(String token, Long userId);

    List<DeviceToken> findAllByUserIdAndActiveTrue(Long userId);
}
