package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Table(
        name = "meeting",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_group_status",
                        columnNames = {"group_id", "tracker_status"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
public class Meeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long meetingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @Column(name = "meeting_time") // 초기에는 null, 트래커 단계에서 업데이트
    private LocalDateTime meetingTime;

    @Column(name = "meeting_place", nullable = false)
    private String meetingPlace;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracker_status", nullable = false)
    private TrackerStatus trackerStatus;

    @Column(name = "host_confirmed", nullable = false)
    private boolean hostConfirmed = false;

    @Column(name = "guest_confirmed", nullable = false)
    private boolean guestConfirmed = false;

    public void confirm(RoleStatus role) {
        if (role == RoleStatus.HOST) this.hostConfirmed = true;
        if (role == RoleStatus.GUEST) this.guestConfirmed = true;
    }
    public boolean isFullyConfirmed() {
        return hostConfirmed && guestConfirmed;
    }

    // 트래커 도메인에서 약속을 확정할 때 사용할 업데이트 메서드
    public void setMeetingDetails(String place, LocalDateTime time) {
        this.meetingPlace = place;
        this.meetingTime = time;
    }

}
