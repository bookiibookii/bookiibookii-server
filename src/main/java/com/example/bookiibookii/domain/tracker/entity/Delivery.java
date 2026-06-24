package com.example.bookiibookii.domain.tracker.entity;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.tracker.enums.DeliveryCompany;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.Objects;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_round", nullable = false)
    private ExchangeRound exchangeRound;

    @Column(name = "tracking_registered_at", nullable = false)
    private Instant trackingRegisteredAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_company")
    private DeliveryCompany deliveryCompany;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_matchedmember_id", nullable = false)
    private MatchedMember sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_matchedmember_id", nullable = false)
    private MatchedMember receiver;

    @Column(name = "received_confirmed_at")
    private Instant receivedConfirmedAt;


    public void complete(Instant deliveredAt) {
        this.deliveredAt = Objects.requireNonNull(deliveredAt, "deliveredAt must not be null");
    }

    public void confirmReceived(Instant confirmedAt) {
        this.receivedConfirmedAt = Objects.requireNonNull(confirmedAt, "confirmedAt must not be null");
    }
}
