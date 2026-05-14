package com.example.bookiibookii.domain.tracker.entity;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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
    private TrackerStatus trackerStatus;

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

    // READY → MY_BOOK_READING (첫 멤버가 읽기 시작)
    public void startFirstReading() {
        if (this.trackerStatus != TrackerStatus.READY) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.MY_BOOK_READING;
        this.startedAt = LocalDateTime.now();
    }

    // EXCHANGED → PARTNER_BOOK_READING (첫 멤버가 2차 읽기 시작)
    public void startSecondReading() {
        if (this.trackerStatus != TrackerStatus.EXCHANGED) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.PARTNER_BOOK_READING;
    }

    // 양측 MY_BOOK_READ_DONE → MY_BOOK_REVIEWING
    public void completeFirstReading() {
        if (this.trackerStatus != TrackerStatus.MY_BOOK_READING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.MY_BOOK_REVIEWING;
    }

    // 양측 PARTNER_BOOK_READ_DONE → PARTNER_BOOK_REVIEWING
    public void completeSecondReading() {
        if (this.trackerStatus != TrackerStatus.PARTNER_BOOK_READING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.PARTNER_BOOK_REVIEWING;
    }

    // MY_BOOK_REVIEWING → EXCHANGING (첫 배송 등록 시)
    public void startExchanging() {
        if (this.trackerStatus != TrackerStatus.MY_BOOK_REVIEWING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.EXCHANGING;
    }

    // EXCHANGING → EXCHANGED (양측 수령 완료)
    public void completeExchange() {
        if (this.trackerStatus != TrackerStatus.EXCHANGING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.EXCHANGED;
    }

    // PARTNER_BOOK_REVIEWING → RETURNING (첫 반납 배송 등록 시)
    public void startReturning() {
        if (this.trackerStatus != TrackerStatus.PARTNER_BOOK_REVIEWING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.RETURNING;
    }

    // RETURNING → COMPLETED (양측 반납 수령 완료)
    public void completeRelay() {
        if (this.trackerStatus != TrackerStatus.RETURNING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

}
