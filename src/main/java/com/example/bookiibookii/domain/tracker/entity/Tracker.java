package com.example.bookiibookii.domain.tracker.entity;

import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tracker extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "group_matching_id")
//    private MatchedGroup matchedGroup;

    @Enumerated(EnumType.STRING)
    private TrackerStatus trackerStatus;

    private Boolean isIssue = false;
    private String issueReason;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Integer extensionCount = 0;
    private Integer extensionDays = 0;


}
