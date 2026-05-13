package com.example.bookiibookii.domain.memberbook.entity;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "member_book",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_book_matchedmember_book",
                columnNames = {"matchedmember_id", "book_id"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberBook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_book_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matchedmember_id", nullable = false)
    private MatchedMember matchedMember;

    /** 독서 진행률(0.0 ~ 100.0) */
    @Column(name = "progress_rate", nullable = false)
    @Builder.Default
    private Double progressRate = 0.0;

    /** 서재에서 제거한 시점. null이면 목록에 노출, 값이 있으면 라이브러리에서 제외 */
    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    public void markRemoved() {
        this.removedAt = LocalDateTime.now();
    }

    public void updateProgressRate(double newRate) {
        this.progressRate = Math.min(100.0, Math.max(0.0, newRate));
    }
}
