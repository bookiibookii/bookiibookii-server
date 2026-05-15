package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.group.enums.ConfirmationStatus;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Table(
        name = "meeting",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tracker_status",
                        columnNames = {"tracker_id", "tracker_status"}
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
    @JoinColumn(name = "tracker_id", nullable = false)
    private Tracker tracker;

    @Enumerated(EnumType.STRING)
    @Column(name = "tracker_status", nullable = false)
    private TrackerStatus trackerStatus;

    @Column(name = "meeting_time")
    private LocalDateTime meetingTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "host_confirmation_status", nullable = false)
    @Builder.Default
    private ConfirmationStatus hostConfirmationStatus = ConfirmationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "guest_confirmation_status", nullable = false)
    @Builder.Default
    private ConfirmationStatus guestConfirmationStatus = ConfirmationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    public void confirm(RoleStatus role) {
        if (role == RoleStatus.HOST) this.hostConfirmationStatus = ConfirmationStatus.CONFIRMED;
        if (role == RoleStatus.GUEST) this.guestConfirmationStatus = ConfirmationStatus.CONFIRMED;
    }

    public boolean isFullyConfirmed() {
        return hostConfirmationStatus == ConfirmationStatus.CONFIRMED &&
                guestConfirmationStatus == ConfirmationStatus.CONFIRMED;
    }

    public void setMeetingDetails(Location location, LocalDateTime time) {
        this.location = location;
        this.meetingTime = time;
    }

    public void resetConfirmation() {
        this.hostConfirmationStatus = ConfirmationStatus.PENDING;
        this.guestConfirmationStatus = ConfirmationStatus.PENDING;
        this.meetingTime = null;
        this.location = null;
    }
}
