package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "application")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Application extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long applicationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private User guest;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status",nullable = false)
    private ApplicationStatus applicationStatus; //PENDING, ACCEPTED, REJECTED


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "apply_msg", length = 200)
    private String applyMsg;

    //신청 상태 변경 메서드
    public void updateStatus(ApplicationStatus status) {
        this.applicationStatus = status;
    }

}
