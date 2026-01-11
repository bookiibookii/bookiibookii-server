package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "`groups`")//예약어 피하기
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Groups extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "group_id")
    private Long groupId;

    //@ManyToOne(fetch = FetchType.LAZY)
    //@Column(name = "book_id")
    //private Book bookId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // 방장(Host) FK 매핑
    private User host;

    @Column(name = "max_capacity")
    private Integer maxCapacity; // 모집 인원

    @Column(name = "start_date") // 시작 날짜
    private LocalDateTime startDate;

    @Column(name = "group_period") // 독서 기간
    private Integer readingPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_status")
    private GroupStatus status; // RECRUITING, MATCHED, COMPLETED

    @Column(name = "group_comment")
    private String groupComment;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<Application> applications = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL)
    private List<MatchedGroup> matchedGroups = new ArrayList<>();

}
