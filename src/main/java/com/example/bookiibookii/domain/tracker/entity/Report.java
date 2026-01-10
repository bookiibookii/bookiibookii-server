package com.example.bookiibookii.domain.tracker.entity;

import com.example.bookiibookii.domain.tracker.enums.IssueType;
import com.example.bookiibookii.domain.tracker.enums.ReportStatus;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    // 어떤 트래커에서 발생한 신고인지 연결
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracker_id")
    private Tracker tracker;

    // 신고자 ID (User 엔티티와 연관관계를 맺거나, ID값만 보관)
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    private IssueType issueType;

    @Column(columnDefinition = "TEXT")
    private String content; // 상세 신고 내용

    @Enumerated(EnumType.STRING)
    private ReportStatus reportStatus;

    @Builder
    public Report(Tracker tracker, Long reporterId, IssueType issueType, String content, ReportStatus reportStatus) {
        this.tracker = tracker;
        this.reporterId = reporterId;
        this.issueType = issueType;
        this.content = content;
        this.reportStatus = reportStatus;
    }

    // 신고 처리 상태를 변경하는 로직
    public void resolveReport() {
        this.reportStatus = ReportStatus.RESOLVED;
    }

}
