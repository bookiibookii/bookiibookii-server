package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.group.enums.MemberStatus;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "member_status")
    @Builder.Default
    private MemberStatus status = MemberStatus.JOINED;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_status")
    @Builder.Default
    private ReadingStatus readingStatus = ReadingStatus.MY_BOOK_READING;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_status")
    @Builder.Default
    private ExchangeStatus exchangeStatus = ExchangeStatus.NOT_STARTED;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Builder.Default
    @Column(name = "is_review_written", nullable = false)
    private boolean isReviewWritten = false;

    public void updateReadingStatus(ReadingStatus newStatus) {
        this.readingStatus = newStatus;
    }

    public void markReviewAsWritten() {
        this.isReviewWritten = true;
    }

}
