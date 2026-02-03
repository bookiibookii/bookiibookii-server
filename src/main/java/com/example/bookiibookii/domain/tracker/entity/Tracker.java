package com.example.bookiibookii.domain.tracker.entity;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
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
    private Integer extensionCount = 0;
    @Column(nullable = false)
    private Integer extensionDays = 0;

    // 현재 주자를 지목하는 1:1 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matchedmember_id", nullable = false)
    private MatchedMember bookOwner;

    // 히스토리와의 1:N 관계
    @OneToMany(mappedBy = "tracker", cascade = CascadeType.ALL)
    private List<TrackerHistory> histories = new ArrayList<>();

    public TrackerHistory createHistorySnapshot(Long senderId, Long receiverId, String company,
                                        String number, String imageUrl) {
        return TrackerHistory.builder()
                .tracker(this)
                .senderMatchedMemberId(senderId)
                .receiverMatchedMemberId(receiverId)
                .trackerStatus(this.trackerStatus) // 변경 전 현재 상태
                .startDate(this.startDate)
                .endDate(this.endDate)
                .deliveryCompany(company)
                .trackingNumber(number)
                .imageUrl(imageUrl)
                .build();
    }

    public void updateShippingStatus(MatchedMember bookOwner, MatchedMember nextOwner) {

        if (this.trackerStatus != TrackerStatus.HOST_DONE &&
                this.trackerStatus != TrackerStatus.GUEST_DONE &&
                this.trackerStatus != TrackerStatus.READ_DONE) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        // 1. 방장이 게스트에게 보내는 경우 (HOST -> GUEST)
        if (bookOwner.getRole() == RoleStatus.HOST) {
            this.trackerStatus = TrackerStatus.SHIPPING_TO_GUEST;
            //알람
        }
        // 2. 마지막 게스트가 호스트에게 보내는 경우 (GUEST -> HOST)
        // 다음 주자의 역할이 HOST라면 마지막 게스트가 보낸 것으로 판단
        else if (nextOwner.getRole() == RoleStatus.HOST) {
            this.trackerStatus = TrackerStatus.SHIPPING_TO_HOST;
        }
        // 3. 게스트가 게스트에게 보내는 경우 (GUEST -> GUEST)
        else {
            this.trackerStatus = TrackerStatus.SHIPPING;
        }

        // 배송 등록 시점 기록
        this.startDate = LocalDateTime.now();

        this.endDate = null;

        // 공통 업데이트: 현재 관리 주자를 다음 사람으로 변경
        this.bookOwner = nextOwner;
    }

    public void updateReceiveStatus() {
        if (this.trackerStatus == TrackerStatus.SHIPPING_TO_HOST) {
            this.trackerStatus = TrackerStatus.RETURNED; // 호스트가 돌려받음
        } else if (this.trackerStatus == TrackerStatus.SHIPPING_TO_GUEST || this.trackerStatus == TrackerStatus.SHIPPING) {
            this.trackerStatus = TrackerStatus.RECEIVED; // 게스트가 전달받음
        } else {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        // 수령 시점 기록
        this.endDate = LocalDateTime.now();

        // 기간 연장 횟수, 일수 초기화.
        this.extensionDays = 0;
        this.extensionCount = 0;

    }


    public void startReading() {
        if (this.trackerStatus == TrackerStatus.RECEIVED) {
            this.trackerStatus = TrackerStatus.GUEST_READING;
            // 독서 시작 시점으로 타이머 리셋
            this.startDate = LocalDateTime.now();
            // 독서 종료 시점 = 독서 시작 시점 + 그룹 독서 기간.
            int readingPeriod = this.group.getReadingPeriod();
            this.endDate = this.startDate.plusDays(readingPeriod);
        } else if (this.trackerStatus == TrackerStatus.READY) {
            this.trackerStatus = TrackerStatus.HOST_READING;
        } else {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

    }

    public void completeReading() {
        if (this.trackerStatus == TrackerStatus.GUEST_READING) {
            this.trackerStatus = TrackerStatus.GUEST_DONE;
        } else if (this.trackerStatus == TrackerStatus.HOST_READING) {
            this.trackerStatus = TrackerStatus.HOST_DONE;
        } else if(this.trackerStatus == TrackerStatus.READING) {
            this.trackerStatus = TrackerStatus.READ_DONE;
        }  else{
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        // 실제 독서 종료 시점 기록
        this.endDate = LocalDateTime.now();
    }

    public void extensionDays(int days) {
        if(days <= 0){
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_DAYS);
        }

        if(this.trackerStatus != TrackerStatus.HOST_READING &&
        this.trackerStatus != TrackerStatus.GUEST_READING &&
        this.trackerStatus != TrackerStatus.READING){
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        // 1. 최대 연장 횟수 초과 체크( 현재는 최대 한번만 연장 가능)
        if (this.extensionCount >= 1) {
            throw new TrackerException(TrackerErrorCode.EXTENSION_LIMIT_EXCEEDED);
        }

        // 2. 마감일(endDate) 연장
        this.endDate = this.endDate.plusDays(days);

        // 3. 연장 정보 업데이트
        this.extensionCount += 1;
        this.extensionDays += days; // 총 연장된 전체 일수 누적
    }

    public void completeRelay() {
        if (this.trackerStatus != TrackerStatus.RETURNED) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.trackerStatus = TrackerStatus.COMPLETED;
    }


    public void updateStatus(TrackerStatus newStatus) {
        if (newStatus == null) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        if (this.trackerStatus == TrackerStatus.COMPLETED) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        this.trackerStatus = newStatus;
    }

    public void transferOwner(MatchedMember nextOwner) {

        if (nextOwner == null) {
            throw new TrackerException(TrackerErrorCode.NEXT_MEMBER_NOT_FOUND);
        }

        if (this.bookOwner.equals(nextOwner)) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }
        this.bookOwner = nextOwner;
    }

}
