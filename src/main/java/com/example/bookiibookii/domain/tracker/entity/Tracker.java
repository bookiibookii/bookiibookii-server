package com.example.bookiibookii.domain.tracker.entity;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tracker")
public class Tracker extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tracker_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReadingStatus readingStatus;

    @Column(nullable = false)
    private LocalDateTime startDate;

    private LocalDateTime endDate;       // 예정 종료일 (그룹 설정 + 연장 반영)

    private LocalDateTime startedAt;     // 실제 독서 시작 시각

    private LocalDateTime completedAt;   // 실제 완료 시각

    @Column(nullable = false)
    @Builder.Default
    private Integer extensionCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer extensionDays = 0;

    @Builder.Default
    @OneToMany(mappedBy = "tracker", cascade = CascadeType.ALL)
    private List<Delivery> deliveries = new ArrayList<>();



    // 양측 MY_BOOK_READ_DONE → MY_BOOK_REVIEWING
    public void completeFirstReading() {
        if (this.readingStatus != ReadingStatus.MY_BOOK_READING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.readingStatus = ReadingStatus.MY_BOOK_REVIEWING;
    }

    // 양측 PARTNER_BOOK_READ_DONE → PARTNER_BOOK_REVIEWING
    public void completeSecondReading() {
        if (this.readingStatus != ReadingStatus.PARTNER_BOOK_READING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.readingStatus = ReadingStatus.PARTNER_BOOK_REVIEWING;
    }

    // MY_BOOK_REVIEWING → EXCHANGING (첫 배송 등록 시)
    public void startExchanging() {
        if (this.readingStatus != ReadingStatus.MY_BOOK_REVIEWING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.readingStatus = ReadingStatus.EXCHANGING;
    }

    // EXCHANGING → EXCHANGED (양측 수령 완료)
    public void completeExchange() {
        if (this.readingStatus != ReadingStatus.EXCHANGING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.readingStatus = ReadingStatus.EXCHANGED;
    }

    // PARTNER_BOOK_REVIEWING → RETURNING (첫 반납 배송 등록 시)
    public void startReturning() {
        if (this.readingStatus != ReadingStatus.PARTNER_BOOK_REVIEWING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.readingStatus = ReadingStatus.RETURNING;
    }

    // RETURNING → COMPLETED (양측 반납 수령 완료)
    public void completeRelay() {
        if (this.readingStatus != ReadingStatus.RETURNING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.readingStatus = ReadingStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void extendReadingPeriod(LocalDate newEndDate) {
        if (this.readingStatus != ReadingStatus.MY_BOOK_READING
                && this.readingStatus != ReadingStatus.PARTNER_BOOK_READING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        if (!newEndDate.isAfter(LocalDate.now())) {
            throw new TrackerException(TrackerErrorCode.INVALID_EXTENSION_DATE);
        }

        LocalDate currentEnd = this.endDate != null ? this.endDate.toLocalDate() : newEndDate;
        int daysDiff = (int) ChronoUnit.DAYS.between(currentEnd, newEndDate);

        this.endDate = newEndDate.atStartOfDay();
        this.extensionCount = this.extensionCount + 1;
        this.extensionDays = this.extensionDays + daysDiff;
    }

}
