package com.example.bookiibookii.domain.user.entity;

import com.example.bookiibookii.domain.user.enums.AgeGroup;
import com.example.bookiibookii.domain.user.enums.Gender;
import com.example.bookiibookii.domain.user.enums.WithdrawalReason;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "user_withdrawal")
public class UserWithdrawal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawal_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WithdrawalReason reason;

    @Column(name = "custom_reason", length = 500)
    private String customReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false, length = 20)
    private AgeGroup ageGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;
}
