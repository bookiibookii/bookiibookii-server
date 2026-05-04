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

    private LocalDateTime endDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer extensionCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer extensionDays = 0;

    @Builder.Default
    @OneToMany(mappedBy = "tracker", cascade = CascadeType.ALL)
    private List<Delivery> deliveries = new ArrayList<>();

    // READY → READING (첫 멤버가 읽기 시작)
    public void startFirstReading() {
        if (this.trackerStatus != TrackerStatus.READY) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.READING;
        this.startDate = LocalDateTime.now();
    }

    // EXCHANGED → READING_2 (첫 멤버가 2차 읽기 시작)
    public void startSecondReading() {
        if (this.trackerStatus != TrackerStatus.EXCHANGED) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.READING_2;
    }

    // 양측 REVIEW_DONE → READ_DONE
    public void completeFirstReading() {
        if (this.trackerStatus != TrackerStatus.READING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.READ_DONE;
    }

    // 양측 REVIEW_DONE_2 → READ_DONE_2
    public void completeSecondReading() {
        if (this.trackerStatus != TrackerStatus.READING_2) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.READ_DONE_2;
    }

    // READ_DONE → EXCHANGING (첫 배송 등록 시)
    public void startExchanging() {
        if (this.trackerStatus != TrackerStatus.READ_DONE) {
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

    // READ_DONE_2 → RETURNING (첫 반납 배송 등록 시)
    public void startReturning() {
        if (this.trackerStatus != TrackerStatus.READ_DONE_2) {
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
        this.endDate = LocalDateTime.now();
    }

    public void updateStatus(TrackerStatus newStatus) {
        if (newStatus == null || this.trackerStatus == TrackerStatus.COMPLETED) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = newStatus;
    }

    public void extensionDays(int days) {
        if (days <= 0) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_DAYS);
        }
        if (this.trackerStatus != TrackerStatus.READING && this.trackerStatus != TrackerStatus.READING_2) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        if (this.extensionCount >= 1) {
            throw new TrackerException(TrackerErrorCode.EXTENSION_LIMIT_EXCEEDED);
        }
        if (this.endDate != null) {
            this.endDate = this.endDate.plusDays(days);
        }
        this.extensionCount += 1;
        this.extensionDays += days;
    }
}
