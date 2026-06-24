package com.example.bookiibookii.domain.user.entity;

import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "profile_share_token",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_profile_share_token_token",
                columnNames = {"token"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProfileShareToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_share_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, length = 36, unique = true)
    private String token;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    public static ProfileShareToken create(User user) {
        return ProfileShareToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .build();
    }

    public void revoke(Instant revokedAt) {
        if (revokedAt == null) {
            throw new IllegalArgumentException("revokedAt must not be null");
        }
        this.revokedAt = revokedAt;
    }

    public boolean isActive() {
        return revokedAt == null;
    }
}
