package com.example.bookiibookii.domain.review.entity;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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

    public void updateReview(Double star, String comment) {
        this.star = star;
        this.comment = comment;
    }
}
