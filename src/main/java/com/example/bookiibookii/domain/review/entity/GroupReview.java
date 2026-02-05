package com.example.bookiibookii.domain.review.entity;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.user.enums.Badge;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "group_review",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_group_review_reviewer_matchedmember",
                        columnNames = {"reviewer_matchedmember_id"}
                ),
                @UniqueConstraint(
                        name = "uk_group_review_reviewed_matchedmember",
                        columnNames = {"reviewed_matchedmember_id"}
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_matchedmember_id", nullable = false)
    private MatchedMember reviewer; // 리뷰 작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_matchedmember_id", nullable = false)
    private MatchedMember reviewed; // 리뷰 수신자

    @Column(name = "rating")
    private Double rating;

    @Column(name = "comment", length = 200)
    private String comment;

    @Builder.Default
    @OneToMany(mappedBy = "groupReview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupReviewBadge> badges = new ArrayList<>();

    public void addBadge(Badge badge) {
        boolean exists = this.badges.stream()
                .anyMatch(grb -> grb.getBadge() == badge);
        if (!exists) {
            this.badges.add(GroupReviewBadge.of(this, badge));
        }
    }
}
