package com.example.bookiibookii.domain.push.entity;

import com.example.bookiibookii.domain.push.enums.DevicePlatform;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(
        name = "device_token",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_token_token", columnNames = "token")
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeviceToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, length = 512)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private DevicePlatform platform;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    public static DeviceToken register(User user, String token, DevicePlatform platform, Instant usedAt) {
        return DeviceToken.builder()
                .user(user)
                .token(token)
                .platform(platform)
                .active(true)
                .lastUsedAt(usedAt)
                .build();
    }

    public void refresh(User user, DevicePlatform platform, Instant usedAt) {
        this.user = user;
        this.platform = platform;
        this.active = true;
        this.lastUsedAt = usedAt;
    }

    public void deactivate() {
        this.active = false;
    }
}
