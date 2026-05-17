package com.example.bookiibookii.domain.memberbook.entity;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "member_book",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_book_matchedmember_book_is_mine",
                columnNames = {"matchedmember_id", "book_id", "is_mine"}
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

    /**
     * MatchedMember 기준으로 이 MemberBook이 본인 책인지 여부.
     * 같은 Book도 내 책과 상대 책 역할로 동시에 존재할 수 있으므로 책 ID로 추론하지 않습니다.
     */
    @Column(name = "is_mine", nullable = false)
    @Builder.Default
    private boolean isMine = false;

    /** 현재 읽은 페이지 */
    @Column(name = "current_page", nullable = false)
    @Builder.Default
    private Integer currentPage = 0;

    /** 서재에서 제거한 시점. null이면 목록에 노출, 값이 있으면 라이브러리에서 제외 */
    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Builder.Default
    @OneToMany(mappedBy = "memberBook", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cards> cards = new ArrayList<>();

    public void markRemoved() {
        this.removedAt = LocalDateTime.now();
    }

    public void updateCurrentPage(int currentPage) {
        this.currentPage = Math.max(0, currentPage);
    }

    public boolean isOwnedBy(MatchedMember matchedMember) {
        return this.matchedMember.getId().equals(matchedMember.getId());
    }

    public boolean isMyBook() {
        return isMine;
    }
}
