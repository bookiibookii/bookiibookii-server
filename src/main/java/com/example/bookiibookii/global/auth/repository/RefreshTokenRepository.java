package com.example.bookiibookii.global.auth.repository;

import com.example.bookiibookii.global.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // userId 기준 RefreshToken 조회
    Optional<RefreshToken> findByUserId(Long userId);

    // userId 기준 RefreshToken 삭제
    void deleteByUserId(Long userId);
}
