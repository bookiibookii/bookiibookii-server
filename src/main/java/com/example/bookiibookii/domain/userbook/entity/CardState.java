package com.example.bookiibookii.domain.userbook.entity;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자별 카드 상태 (북마크 / 내 화면에서 숨김).
 * user-card 쌍당 하나의 행으로 북마크·숨김을 한 엔티티에서 관리한다.
 */
@Entity
@Table(
        name = "card_state",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "card_id"})
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardState extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_state_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(name = "bookmarked", nullable = false)
    @Builder.Default
    private boolean bookmarked = false;

    @Column(name = "hidden", nullable = false)
    @Builder.Default
    private boolean hidden = false;

    public void setBookmarked(boolean bookmarked) {
        this.bookmarked = bookmarked;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
}
