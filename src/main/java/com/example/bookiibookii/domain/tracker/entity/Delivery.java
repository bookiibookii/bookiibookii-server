package com.example.bookiibookii.domain.tracker.entity;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.tracker.enums.DeliveryCompany;
import com.example.bookiibookii.domain.tracker.enums.DeliveryStatus;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "delivery",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_delivery_group_round_sender",
                columnNames = {"group_id", "exchange_round", "sender_matchedmember_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Delivery extends BaseEntity {

    @Id
    @Column(name = "delivery_id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Groups group;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_round")
    private ExchangeRound exchangeRound;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status")
    private DeliveryStatus deliveryStatus;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_company")
    private DeliveryCompany deliveryCompany;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracker_id")
    private Tracker tracker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_matchedmember_id")
    private MatchedMember sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_matchedmember_id", nullable = false)
    private MatchedMember receiver;

    @Column(name = "received_confirmed_at")
    private LocalDateTime receivedConfirmedAt;

    @Builder.Default
    @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrackingImage> trackingImages = new ArrayList<>();

    public void complete(LocalDateTime completedAt) {
        this.endDate = completedAt;
        this.deliveryStatus = DeliveryStatus.RETURNED;
    }

    public void confirmReceived(LocalDateTime confirmedAt) {
        this.receivedConfirmedAt = confirmedAt;
        this.endDate = confirmedAt;
        this.deliveryStatus = DeliveryStatus.RETURNED;
    }
}
