package com.example.bookiibookii.domain.tracker.entity;


import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackerHistory extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tracker_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracker_id", nullable = false)
    private Tracker tracker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackerStatus trackerStatus;

    private String deliveryCompany;
    private String trackingNumber;
    private String imageUrl;

    @Builder
    public TrackerHistory(Tracker tracker, TrackerStatus trackerStatus, String deliveryCompany, String trackingNumber, String imageUrl) {
        this.tracker = tracker;
        this.trackerStatus = trackerStatus;
        this.deliveryCompany = deliveryCompany;
        this.trackingNumber = trackingNumber;
        this.imageUrl = imageUrl;
    }

}
