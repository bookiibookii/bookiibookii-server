package com.example.bookiibookii.domain.tracker.entity;


import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
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
@Table(name = "tracker_history")
public class TrackerHistory extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tracker_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracker_id", nullable = false)
    private Tracker tracker;

    private Long senderMatchedMemberId;

    @Column(nullable = false)
    private Long receiverMatchedMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackerStatus trackerStatus;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String deliveryCompany;
    private String trackingNumber;
    private String imageUrl;

    @OneToMany(mappedBy = "trackerHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TrackerImage> trackerImages = new ArrayList<>();

    public static TrackerHistory createHistory(Tracker tracker, Long senderId, Long receiverId,
                                               TrackerStatus status, LocalDateTime startDate, LocalDateTime endDate,
                                               String company, String number) {
        return TrackerHistory.builder()
                .tracker(tracker)
                .senderMatchedMemberId(senderId)
                .receiverMatchedMemberId(receiverId)
                .trackerStatus(status)
                .startDate(startDate)
                .endDate(endDate)
                .deliveryCompany(company)
                .trackingNumber(number)
                .build();
    }


}
