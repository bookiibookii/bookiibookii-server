package com.example.bookiibookii.domain.groupbook.entity;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "group_book")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupBook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_book_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracker_id", nullable = true)
    private Tracker tracker;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "comment", length = 500)
    private String comment;

    /** 서재에서 제거한 시점. null이면 목록에 노출, 값이 있으면 라이브러리에서 제외 */
    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    public void updateReview(Double rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    /** 서재에서만 제거(소프트 삭제??). 다른 멤버는 계속 조회 가능. */
    public void markRemoved() {
        this.removedAt = LocalDateTime.now();
    }


    // 나중에 트래커 할당
    public void assignTracker(Tracker tracker) {
        this.tracker = tracker;
    }
}
