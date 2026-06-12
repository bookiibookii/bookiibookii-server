package com.example.bookiibookii.domain.group.entity;

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

import java.math.BigDecimal;
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

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "x", nullable = false, precision = 16, scale = 10)
    private BigDecimal x;

    @Column(name = "y", nullable = false, precision = 16, scale = 10)
    private BigDecimal y;

    @Column(name = "address_detail", length = 200)
    private String addressDetail;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    public static Meeting create(
            Groups group,
            MatchedMember createdBy,
            ExchangeRound exchangeRound,
            String placeName,
            String address,
            String zipCode,
            BigDecimal x,
            BigDecimal y,
            String addressDetail,
            LocalDateTime scheduledAt
    ) {
        validate(group, createdBy, exchangeRound, placeName, address, x, y, scheduledAt);

        return Meeting.builder()
                .group(group)
                .createdBy(createdBy)
                .exchangeRound(exchangeRound)
                .placeName(normalize(placeName))
                .address(normalize(address))
                .zipCode(normalizeNullable(zipCode))
                .x(x)
                .y(y)
                .addressDetail(normalizeNullable(addressDetail))
                .scheduledAt(scheduledAt)
                .build();
    }

    public void update(
            String placeName,
            String address,
            String zipCode,
            BigDecimal x,
            BigDecimal y,
            String addressDetail,
            LocalDateTime scheduledAt
    ) {
        Objects.requireNonNull(placeName, "placeName must not be null");
        Objects.requireNonNull(address, "address must not be null");
        Objects.requireNonNull(x, "x must not be null");
        Objects.requireNonNull(y, "y must not be null");
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");

        this.placeName = normalize(placeName);
        this.address = normalize(address);
        this.zipCode = normalizeNullable(zipCode);
        this.x = x;
        this.y = y;
        this.addressDetail = normalizeNullable(addressDetail);
        this.scheduledAt = scheduledAt;
    }

    public boolean hasSameScheduleAndPlace(
            String placeName,
            String address,
            String zipCode,
            BigDecimal x,
            BigDecimal y,
            String addressDetail,
            LocalDateTime scheduledAt
    ) {
        return Objects.equals(normalize(this.placeName), normalize(placeName))
                && Objects.equals(normalize(this.address), normalize(address))
                && Objects.equals(normalizeNullable(this.zipCode), normalizeNullable(zipCode))
                && sameNumber(this.x, x)
                && sameNumber(this.y, y)
                && Objects.equals(normalizeNullable(this.addressDetail), normalizeNullable(addressDetail))
                && Objects.equals(this.scheduledAt, scheduledAt);
    }

    private boolean sameNumber(BigDecimal current, BigDecimal requested) {
        return current == null ? requested == null : requested != null && current.compareTo(requested) == 0;
    }

    private static String normalize(String value) {
        return Objects.requireNonNull(value).trim();
    }

    private static String normalizeNullable(String value) {
        return value == null ? null : value.trim();
    }

    private static void validate(
            Groups group,
            MatchedMember createdBy,
            ExchangeRound exchangeRound,
            String placeName,
            String address,
            BigDecimal x,
            BigDecimal y,
            LocalDateTime scheduledAt
    ) {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(createdBy, "createdBy must not be null");
        Objects.requireNonNull(exchangeRound, "exchangeRound must not be null");
        Objects.requireNonNull(placeName, "placeName must not be null");
        Objects.requireNonNull(address, "address must not be null");
        Objects.requireNonNull(x, "x must not be null");
        Objects.requireNonNull(y, "y must not be null");
        Objects.requireNonNull(scheduledAt, "scheduledAt must not be null");
    }
}
