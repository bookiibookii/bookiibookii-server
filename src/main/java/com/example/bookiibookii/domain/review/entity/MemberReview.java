package com.example.bookiibookii.domain.review.entity;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
        name = "member_review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_review_group_writer",
                columnNames = {"group_id", "writer_matched_member_id"}
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
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_matched_member_id", nullable = false)
    private MatchedMember writer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_matched_member_id", nullable = false)
    private MatchedMember target;

    @Enumerated(EnumType.STRING)
    @Column(name = "reaction", length = 30)
    private MemberReviewReaction reaction;

    @Column(name = "comment", nullable = false, length = 20)
    private String comment;

    public static MemberReview create(
            Groups group,
            MatchedMember writer,
            MatchedMember target,
            MemberReviewReaction reaction,
            String comment
    ) {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(writer, "writer must not be null");
        Objects.requireNonNull(target, "target must not be null");
        validateComment(comment);

        return MemberReview.builder()
                .group(group)
                .writer(writer)
                .target(target)
                .reaction(reaction)
                .comment(comment)
                .build();
    }

    private static void validateComment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("comment must not be blank");
        }
        if (comment.length() > 20) {
            throw new IllegalArgumentException("comment cannot exceed 20 characters");
        }
    }
}
