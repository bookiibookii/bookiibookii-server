package com.example.bookiibookii.domain.user.entity;

import com.example.bookiibookii.domain.user.enums.Badge;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_badge")
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserBadge extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_code", nullable = false)
    private Badge badge;

    @Column(name = "count", nullable = false)
    @Builder.Default
    private Integer count = 0;

    // 카운트 증가 메서드
    public void increaseCount() {
        this.count++;
    }
}
