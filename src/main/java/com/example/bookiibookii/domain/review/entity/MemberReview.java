package com.example.bookiibookii.domain.review.entity;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "member_review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_review_writer_target",
                columnNames = {"writer_matched_member_id", "target_matched_member_id"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_matched_member_id", nullable = false)
    private MatchedMember writer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_matched_member_id", nullable = false)
    private MatchedMember target;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction", length = 30)
    private MemberReviewReaction reaction;

    @Column(name = "comment", length = 255)
    private String comment;

    public void updateReview(MemberReviewReaction reaction, String comment) {
        if (comment != null && comment.length() > 255) {
            throw new IllegalArgumentException("Comment cannot exceed 255 characters");
        }
        this.reaction = reaction;
        this.comment = comment;
    }
}
