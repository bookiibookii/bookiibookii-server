package com.example.bookiibookii.domain.location.entity;

import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "location")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Location extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long id;

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

    public void fillMissingDetails(String zipCode) {
        if (this.zipCode == null && zipCode != null) {
            this.zipCode = zipCode;
        }
    }
}
