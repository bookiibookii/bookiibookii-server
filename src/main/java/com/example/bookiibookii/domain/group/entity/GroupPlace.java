package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.group.enums.GroupPlaceSourceType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "group_place")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupPlace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_place_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, unique = true)
    private Groups group;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false)
    private GroupPlaceSourceType sourceType;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "x", precision = 16, scale = 10)
    private BigDecimal x;

    @Column(name = "y", precision = 16, scale = 10)
    private BigDecimal y;

    @Column(name = "address_detail", length = 200)
    private String addressDetail;

    @Column(name = "receiver_name", length = 50)
    private String receiverName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;
}
