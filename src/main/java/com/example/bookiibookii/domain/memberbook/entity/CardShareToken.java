package com.example.bookiibookii.domain.memberbook.entity;

import com.example.bookiibookii.domain.memberbook.enums.ShareLayout;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "card_share_token",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_card_share_token_token",
                columnNames = {"token"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardShareToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_share_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Cards card;

    @Column(name = "token", nullable = false, length = 36, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "share_layout", nullable = false, length = 20)
    private ShareLayout shareLayout;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    public static CardShareToken create(Cards card, User createdBy, ShareLayout shareLayout) {
        return CardShareToken.builder()
                .card(card)
                .token(UUID.randomUUID().toString())
                .createdBy(createdBy)
                .shareLayout(shareLayout)
                .build();
    }

    public void revoke(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public boolean isActive() {
        return revokedAt == null;
    }
}
