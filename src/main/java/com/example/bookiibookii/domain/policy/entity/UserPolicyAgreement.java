package com.example.bookiibookii.domain.policy.entity;

import com.example.bookiibookii.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(
        name = "user_policy_agreement",
        indexes = {
                @Index(
                        name = "idx_user_policy_agreement_user_policy",
                        columnList = "user_id, policy_document_id"
                ),
                @Index(
                        name = "idx_user_policy_agreement_user_created_at",
                        columnList = "user_id, acted_at"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserPolicyAgreement {

    // 유저 약관동의 새로 업데이트 시
    // update 아닌 userPolicyAgreement 객체 insert

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(name = "policy_document_id", nullable = false)
    private PolicyDocument policyDocument;

    @Column(name = "agreed", nullable = false)
    private boolean agreed;

    @Column(name = "acted_at")
    private LocalDateTime actedAt;

    public static UserPolicyAgreement agree(User user, PolicyDocument policyDocument) {
        return UserPolicyAgreement.builder()
                .user(user)
                .policyDocument(policyDocument)
                .agreed(true)
                .actedAt(LocalDateTime.now())
                .build();
    }

    public static UserPolicyAgreement disagree(User user, PolicyDocument policyDocument) {
        return UserPolicyAgreement.builder()
                .user(user)
                .policyDocument(policyDocument)
                .agreed(false)
                .actedAt(LocalDateTime.now())
                .build();
    }
}