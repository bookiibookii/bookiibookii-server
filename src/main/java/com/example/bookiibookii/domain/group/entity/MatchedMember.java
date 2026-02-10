package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "matchedmember")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MatchedMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "matchedmember_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Groups group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private RoleStatus role;

    @Column(name = "reading_order", nullable = false)
    private Integer readingOrder; // 1, 2, 3... 순서 저장

    @Column(name = "current_reading_rate", nullable = false)
    private Integer currentReadingRate = 0;

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 독서 종료일 기록

    @Column(name = "is_review_written", nullable = false)
    private boolean isReviewWritten = false; // 리뷰 작성 여부 추가

    public void markReviewAsWritten() {
        this.isReviewWritten = true;
    }

    public void updateReadingRate(int newRate) {
        int normalized = Math.min(100, Math.max(0,newRate));
        this.currentReadingRate = normalized;
    }

    public void completeReading() {
        this.currentReadingRate = 100;
        this.completedAt = LocalDateTime.now();
    }
}
