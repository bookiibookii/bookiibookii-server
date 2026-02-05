package com.example.bookiibookii.domain.review.entity;

import com.example.bookiibookii.domain.user.enums.Badge;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "group_review_badge",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_group_review_badge",
                columnNames = {"group_review_id", "badge_code"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupReviewBadge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_review_badge_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_review_id")
    private GroupReview groupReview;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_code", nullable = false)
    private Badge badge;

    public static GroupReviewBadge of(GroupReview groupReview, Badge badge) {
        return GroupReviewBadge.builder()
                .groupReview(groupReview)
                .badge(badge)
                .build();
    }
}
