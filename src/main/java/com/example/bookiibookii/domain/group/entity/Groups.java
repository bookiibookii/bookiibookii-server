package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
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
    private TradeType tradeType;//DIRECT, DELIVERY

    @Column(name = "prefer_region")
    private String preferRegion;

    @Column(name = "group_name")
    private String groupName;

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<Application> applications = new ArrayList<>();

    @Builder.Default
    @BatchSize(size = 10)
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<MatchedMember> matchedMember = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<GroupBook> groupBooks = new ArrayList<>();

    @OneToOne(mappedBy = "group", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private GroupPlace groupPlace;

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
}
