package com.example.bookiibookii.domain.tracker.entity;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.tracker.enums.DeliveryStatus;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "delivery")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Delivery extends BaseEntity {

    @Id
    @Column(name = "delivery_id")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    private DeliveryStatus deliveryStatus;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "delivery_company")
    private String deliveryCompany;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracker_id", nullable = false)
    private Tracker tracker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_matchedmember_id")
    private MatchedMember sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_matchedmember_id", nullable = false)
    private MatchedMember receiver;

    public void complete(LocalDateTime completedAt) {
        this.endDate = completedAt;
        this.deliveryStatus = DeliveryStatus.RETURNED;
    }
}
