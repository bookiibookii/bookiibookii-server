package com.example.bookiibookii.domain.tracker.entity;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.global.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tracker")
public class Tracker extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tracker_id")
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

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

    // 현재 주자를 지목하는 1:1 관계
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matchedmember_id", nullable = false)
    private MatchedMember currentMember;

    // 히스토리와의 1:N 관계
    @OneToMany(mappedBy = "tracker", cascade = CascadeType.ALL)
    private List<TrackerHistory> histories = new ArrayList<>();


    // 상태 변경 메서드
    public void updateCurrentMember(MatchedMember nextMember, TrackerStatus status) {
        this.currentMember = nextMember;
        this.trackerStatus = status;
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
