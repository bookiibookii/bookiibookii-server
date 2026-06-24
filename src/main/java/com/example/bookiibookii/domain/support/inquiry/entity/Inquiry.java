package com.example.bookiibookii.domain.support.inquiry.entity;

import com.example.bookiibookii.domain.support.inquiry.enums.SupportStatus;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "inquiry")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Inquiry extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "content", length = 255, nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SupportStatus supportStatus = SupportStatus.PENDING;

    @Column(name = "admin_reply", length = 255)
    private String adminReply;

    private Instant resolvedAt;


    /**
     * 관리자 답변 등록 및 상태 변경
     */
    public void updateAnswer(String adminReply, Instant resolvedAt) {
        this.adminReply = adminReply;
        this.supportStatus = SupportStatus.RESOLVED;
        this.resolvedAt = resolvedAt;
    }
}
