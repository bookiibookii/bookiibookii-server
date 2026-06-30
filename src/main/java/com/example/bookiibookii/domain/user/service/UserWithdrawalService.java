package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.MemberStatus;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserWithdrawal;
import com.example.bookiibookii.domain.user.enums.AgeGroup;
import com.example.bookiibookii.domain.user.enums.Gender;
import com.example.bookiibookii.domain.user.enums.WithdrawalReason;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserWithdrawalRepository;
import com.example.bookiibookii.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserWithdrawalService {

    private final UserWithdrawalRepository userWithdrawalRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final RedisUtil redisUtil;

    private static final List<GroupStatus> ACTIVE_GROUP_STATUSES = List.of(GroupStatus.RECRUITING, GroupStatus.MATCHED);

    public void withdraw(User user, WithdrawalReason reason, String customReason) {
        if (matchedMemberRepository.existsByUser_IdAndStatusAndGroup_GroupStatusIn(
                user.getId(), MemberStatus.JOINED, ACTIVE_GROUP_STATUSES)) {
            throw new UserException(UserErrorCode.ACTIVE_GROUP_EXISTS);
        }

        if (reason == null) {
            throw new IllegalArgumentException("탈퇴 사유는 필수입니다.");
        }
        if (reason == WithdrawalReason.CUSTOM_INPUT && (customReason == null || customReason.isBlank())) {
            throw new IllegalArgumentException("직접 입력 사유는 필수입니다.");
        }
        if (reason != WithdrawalReason.CUSTOM_INPUT) {
            customReason = null;
        }

        AgeGroup ageGroup = user.getBirth() != null
                ? AgeGroup.from(user.getBirth())
                : AgeGroup.FORTIES_AND_ABOVE;

        Gender gender = user.getGender() != null
                ? user.getGender()
                : Gender.NONE;

        UserWithdrawal withdrawal = UserWithdrawal.builder()
                .reason(reason)
                .customReason(customReason)
                .ageGroup(ageGroup)
                .gender(gender)
                .build();

        userWithdrawalRepository.save(withdrawal);
        redisUtil.delete("RT:" + user.getId());
        user.withdraw();
    }
}
