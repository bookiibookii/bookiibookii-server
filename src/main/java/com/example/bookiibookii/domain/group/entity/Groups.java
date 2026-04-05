package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.Tag;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "`groups`")//예약어 피하기
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Groups extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id") // 방장(Host) FK 매핑
    private User host;

    @Column(name = "max_capacity")
    private Integer maxCapacity; // 모집 인원

    @Column(name = "start_date") // 시작 날짜(시간포함x)
    private LocalDate startDate;

    @Column(name = "group_period") // 독서 기간
    private Integer readingPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_status")
    private GroupStatus groupStatus; // RECRUITING, MATCHED, COMPLETED

    @Column(name = "group_comment", length = 200)
    private String groupComment;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type")
    private GroupType groupType; //RELAY, TOGETHER

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type")
    private TradeType tradeType;//DIRECT, NONE

    @Column(name = "prefer_region")
    private String preferRegion;

    @Column(name = "group_name")
    private String groupName;

    @Builder.Default
    @Column(name = "is_private", nullable = false)
    private Boolean isPrivate = false;

    @Column(name = "password")
    private String password;

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<Application> applications = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<MatchedMember> matchedMember = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<UserBook> userBooks = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Meeting> meetings = new ArrayList<>();

    @Builder.Default
    @BatchSize(size = 10)
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupRule> groupRules = new ArrayList<>();

    public void updateStatus(GroupStatus status) {
        this.groupStatus = status;
    }

    public void markAsDELETED() {
        this.groupStatus = GroupStatus.DELETED;
    }

    //그룹상태 계산
    public void syncStatus(long totalMemberCount, LocalDate today) {
        // 1. 이미 종료된 그룹은 건드리지 않음
        if (this.groupStatus == GroupStatus.COMPLETED || this.groupStatus == GroupStatus.DELETED) {
            return;
        }

        // 2. 시작일 도달 여부 체크 (최우선순위)
        if (!today.isBefore(this.startDate)) {
            // 오늘이 시작일이거나 지났다면: 2명 이상(호스트 포함)이면 MATCHED, 아니면 DELETED
            this.groupStatus = (totalMemberCount >= 2) ? GroupStatus.MATCHED : GroupStatus.DELETED;
        }
        // 3. 아직 시작일 전인 경우 (모집 기간)
        else {
            // 정원이 찼으면 MATCHED, 아니면 RECRUITING (취소 시 복구까지 고려)
            this.groupStatus = (totalMemberCount >= this.maxCapacity) ? GroupStatus.MATCHED : GroupStatus.RECRUITING;
        }
    }
}
