package com.example.bookiibookii.domain.location.entity;

import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

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
    private Long locationId;

    @Column(name = "place_name", nullable = false, length = 100)
    private String placeName;

    @Column(name = "address", nullable = false, unique = true, length = 200)
    private String address;

    @Column(name = "zip_code", nullable = false, length = 10)
    private String zipCode;
}
