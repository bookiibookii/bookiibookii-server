package com.example.bookiibookii.global.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_token")
public class RefreshToken {
    @Id
    private Long userId;

    @Column(nullable = false, length = 1000)
    private String token;

    private RefreshToken(Long userId, String token) {
        this.userId = userId;
        this.token = token;
    }

    public static RefreshToken of(Long userId, String token) {
        return new RefreshToken(userId, token);
    }

    public void update(String newToken) {
        this.token = newToken;
    }
}

