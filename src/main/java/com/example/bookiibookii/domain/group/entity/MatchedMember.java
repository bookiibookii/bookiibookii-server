package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.group.enums.MemberStatus;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "matchedmember",
        indexes = {
                @Index(
                        name = "idx_matchedmember_user_id_group_id",
                        columnList = "user_id, group_id"
                )
        }
)
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

    // 그룹 내에서 거치는 책 2권
    @Builder.Default
    @OneToMany(mappedBy = "matchedMember")
    private List<MemberBook> memberBooks = new ArrayList<>();

    // 현재 내가 읽고있는 책 포인터 (1차교환 전 : 내 소유 책 / 1차교환 이후 : 파트너 책)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_member_book_id")
    private MemberBook currentMemberBook;

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

    // 독서 시작 날짜 (교환독서 내 1차 읽는중, 2차 읽는중 시작 시점으로 설정)
    @Column(name = "reading_started_at")
    private Instant readingStartedAt;

    // 이거 모르겠음
    @Column(name = "completed_at")
    private Instant completedAt;

    @Builder.Default
    @Column(name = "is_review_written", nullable = false)
    private boolean isReviewWritten = false;

    @Column(name = "partner_reviewing_started_at")
    private Instant partnerReviewingStartedAt;

    public void updateReadingStatus(ReadingStatus newStatus) {
        this.readingStatus = newStatus;
    }

    public void updateReadingStatus(ReadingStatus newStatus, Instant changedAt) {
        if (this.readingStatus != ReadingStatus.PARTNER_REVIEWING
                && newStatus == ReadingStatus.PARTNER_REVIEWING) {
            if (changedAt == null) {
                throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
            }
            this.partnerReviewingStartedAt = changedAt;
        }
        this.readingStatus = newStatus;
    }

    public void updateExchangeStatus(ExchangeStatus newStatus) {
        this.exchangeStatus = newStatus;
    }

    public void markReviewAsWritten() {
        this.isReviewWritten = true;
    }

    public void completeReading(Instant completedAt) {
        if (completedAt == null) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.readingStatus = ReadingStatus.COMPLETED;
        this.completedAt = completedAt;
    }

    public void startMatchedReading(MemberBook initialBook, Instant matchedAt) {
        validateCurrentBook(initialBook);

        if (!initialBook.isMine()) {
            throw new TrackerException(TrackerErrorCode.INITIAL_CURRENT_BOOK_NOT_MY_BOOK);
        }

        if (matchedAt == null) {
            throw new TrackerException(TrackerErrorCode.MATCHED_AT_REQUIRED);
        }

        this.currentMemberBook = initialBook;
        this.readingStartedAt = matchedAt;
    }
    public void changeCurrentBook(MemberBook nextBook, Instant changedAt) {
        validateCurrentBook(nextBook);

        if (changedAt == null) {
            throw new TrackerException(TrackerErrorCode.INVALID_READING_STARTED_AT);
        }

        if (this.currentMemberBook != nextBook && this.readingStatus == ReadingStatus.EXCHANGING) {
            nextBook.resetProgress();
        }
        this.currentMemberBook = nextBook;
        this.readingStartedAt = changedAt;
    }

    public void changeCurrentMemberBook(MemberBook memberBook, Instant changedAt) {
        changeCurrentBook(memberBook, changedAt);
    }

    private void validateCurrentBook(MemberBook memberBook) {
        if (memberBook == null) {
            throw new TrackerException(TrackerErrorCode.INVALID_CURRENT_MEMBER_BOOK);
        }

        if (!memberBook.isOwnedBy(this)) {
            throw new TrackerException(TrackerErrorCode.INVALID_CURRENT_BOOK_OWNER);
        }
    }
}
