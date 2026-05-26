package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "meeting",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_meeting_group_exchange_round",
                columnNames = {"group_id", "exchange_round"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Meeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "meeting_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_matchedmember_id", nullable = false)
    private MatchedMember createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "exchange_round", nullable = false, length = 30)
    private ExchangeRound exchangeRound;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "address_detail", length = 200)
    private String addressDetail;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    public static Meeting create(
            Groups group,
            MatchedMember createdBy,
            ExchangeRound exchangeRound,
            Location location,
            String addressDetail,
            LocalDateTime scheduledAt
    ) {
        validate(group, createdBy, exchangeRound, location, scheduledAt);

        return Meeting.builder()
                .group(group)
                .createdBy(createdBy)
                .exchangeRound(exchangeRound)
                .location(location)
                .addressDetail(addressDetail)
                .scheduledAt(scheduledAt)
                .build();
    }

    public void update(
            Location location,
            String addressDetail,
            LocalDateTime scheduledAt
    ) {
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");

        this.location = location;
        this.addressDetail = addressDetail;
        this.scheduledAt = scheduledAt;
    }

    private static void validate(
            Groups group,
            MatchedMember createdBy,
            ExchangeRound exchangeRound,
            Location location,
            LocalDateTime scheduledAt
    ) {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
        Objects.requireNonNull(exchangeRound, "exchangeRound must not be null");
        Objects.requireNonNull(location, "location must not be null");
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
    }
}
