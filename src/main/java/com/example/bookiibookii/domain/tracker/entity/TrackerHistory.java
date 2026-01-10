package com.example.bookiibookii.domain.tracker.entity;


import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackerHistory extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "group_matching_id")
//    private MatchedGroup matchedGroup;

    @Enumerated(EnumType.STRING)
    private TrackerStatus trackerStatus;

    private String deliveryCompany;
    private String trackingNumber;
    private String imageUrl;


}
