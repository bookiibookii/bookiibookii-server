package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.MemberStatus;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserWithdrawal;
import com.example.bookiibookii.domain.user.enums.AgeGroup;
import com.example.bookiibookii.domain.user.enums.Gender;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.enums.WithdrawalReason;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.domain.user.repository.UserWithdrawalRepository;
import com.example.bookiibookii.global.auth.social.AppleAuthClient;
import com.example.bookiibookii.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserWithdrawalService {

    private final UserRepository userRepository;
    private final UserWithdrawalRepository userWithdrawalRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final RedisUtil redisUtil;
    private final AppleAuthClient appleAuthClient;

    private static final List<GroupStatus> ACTIVE_GROUP_STATUSES = List.of(GroupStatus.RECRUITING, GroupStatus.MATCHED);

    public void withdraw(User passedUser, WithdrawalReason reason, String customReason) {
        User user = userRepository.findById(passedUser.getId())
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

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

        // entity dirty checking 에 의존하지 않고 직접 UPDATE
        // (OSIV 환경에서 JwtAuthFilter read-only 로드 시 snapshot 없어 dirty check 미동작 문제 방지)
        userRepository.withdrawUser(user.getId());

        // Apple 유저: App Store 심사 지침 준수 — refresh_token revoke (실패해도 탈퇴 진행)
        // withdrawUser() 이후 호출 → clearAppleRefreshToken의 status='WITHDRAWN' 조건 충족
        if (SocialType.APPLE.equals(user.getSocialType()) && user.getAppleRefreshToken() != null) {
            boolean revoked = appleAuthClient.revokeToken(user.getAppleRefreshToken());
            if (revoked) {
                userRepository.clearAppleRefreshToken(user.getId(), user.getAppleRefreshToken());
            }
        }
    }
}
