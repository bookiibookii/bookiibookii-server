package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
public class Meeting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Groups group;

    private LocalDateTime meetingTime;      // 교환희망일시
    private String meetingPlace;            //교환희망장소

}
