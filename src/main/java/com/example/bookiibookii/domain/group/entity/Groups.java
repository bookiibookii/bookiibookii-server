package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tag.entity.Tag;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Column(name = "custom_tag", length = 8)
    private String customTag;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type")
    private GroupType groupType; //RELAY, TOGETHER

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type")
    private TradeType tradeType;//DIRECT, DELIVERY

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<Application> applications = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<MatchedMember> matchedMember = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupTag> groupTags = new ArrayList<>();

    public void updateStatus(GroupStatus status) {
        this.groupStatus = status;
    }

    public void markAsDELETED(){ this.groupStatus = GroupStatus.DELETED; }

    // 태그 추가
    public void addGroupTag(Tag tag) {
        GroupTag groupTag = GroupTag.create(this, tag);
        this.groupTags.add(groupTag);
    }

    // 태그 전체 삭제
    public void clearGroupTags() {
        this.groupTags.clear();
    }
}
