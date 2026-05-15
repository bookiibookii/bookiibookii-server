package com.example.bookiibookii.domain.review.entity;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;

@Entity
@Table(
        name = "book_review",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_book_review_matchedmember_memberbook",
                columnNames = {"matchedmember_id", "member_book_id"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BookReview extends BaseEntity {

    private static final double STAR_MIN = 0.0;
    private static final double STAR_MAX = 5.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matchedmember_id", nullable = false)
    private MatchedMember matchedMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_book_id", nullable = false)
    private MemberBook memberBook;

    @Column(name = "star")
    private Double star;

    @Column(name = "comment", length = 255)
    private String comment;

    public static BookReview create(
            MatchedMember matchedMember,
            MemberBook memberBook,
            Double star,
            String comment
    ) {
        Objects.requireNonNull(matchedMember, "matchedMember must not be null");
        Objects.requireNonNull(memberBook, "memberBook must not be null");
        validateStar(star);
        return BookReview.builder()
                .matchedMember(matchedMember)
                .memberBook(memberBook)
                .star(star)
                .comment(comment)
                .build();
    }

    public void updateReview(Double star, String comment) {
        validateStar(star);
        this.star = star;
        this.comment = comment;
    }

    private static void validateStar(Double star) {
        if (star == null) {
            return;
        }
        if (star < STAR_MIN || star > STAR_MAX) {
            throw new IllegalArgumentException(
                    "star must be between " + STAR_MIN + " and " + STAR_MAX
            );
        }
    }
}
