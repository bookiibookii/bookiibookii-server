package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserWithdrawal;
import com.example.bookiibookii.domain.user.enums.AgeGroup;
import com.example.bookiibookii.domain.user.enums.Gender;
import com.example.bookiibookii.domain.user.enums.WithdrawalReason;
import com.example.bookiibookii.domain.user.repository.UserWithdrawalRepository;
import com.example.bookiibookii.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserWithdrawalService {

    private final UserWithdrawalRepository userWithdrawalRepository;
    private final RedisUtil redisUtil;

    public void withdraw(User user, WithdrawalReason reason, String customReason) {
        AgeGroup ageGroup = user.getBirth() != null
                ? AgeGroup.from(user.getBirth())
                : AgeGroup.FORTIES_AND_ABOVE;

        Gender gender = user.getGender() != null
                ? user.getGender()
                : Gender.NONE;

        UserWithdrawal withdrawal = UserWithdrawal.builder()
                .reason(reason)
                .customReason(reason == WithdrawalReason.CUSTOM_INPUT ? customReason : null)
                .ageGroup(ageGroup)
                .gender(gender)
                .build();

        userWithdrawalRepository.save(withdrawal);
        redisUtil.delete("RT:" + user.getId());
        user.withdraw();
    }
}
