package com.example.bookiibookii.domain.memberbook.entity;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 매칭 멤버별 카드 상태 (북마크 / 내 화면에서 숨김).
 * matchedmember-card 쌍당 하나의 행으로 북마크·숨김을 한 엔티티에서 관리한다.
 */
@Entity
@Table(
        name = "member_card",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_card_matchedmember_card",
                columnNames = {"matchedmember_id", "card_id"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_state_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Cards card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matchedmember_id", nullable = false)
    private MatchedMember matchedMember;

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
