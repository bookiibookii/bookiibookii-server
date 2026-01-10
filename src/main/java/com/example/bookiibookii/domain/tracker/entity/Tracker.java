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
    @Column(name = "tracker_id")
    private Long id;

//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "group_matching_id")
//    private MatchedGroup matchedGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrackerStatus trackerStatus;

    @Column(nullable = false)
    private Boolean isIssue = false;
    private String issueReason;

    @Column(nullable = false)
    private LocalDateTime startDate;
    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Integer extensionCount = 0;
    @Column(nullable = false)
    private Integer extensionDays = 0;

    // 상태 변경 메서드
    public void changeStatus(TrackerStatus trackerStatus){
        this.trackerStatus = trackerStatus;
    }

    // 이슈 발생 메서드
    public void markIssue(String reason){
        this.isIssue = true;
        this.issueReason = reason;
    }

    // 이슈 해결 메서드
    public void resolveIssue() {
        this.isIssue = false;
        this.issueReason = null;
    }

}
