package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.global.auth.social.SocialUserInfo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;

    // 소셜 유저 조회 or 생성
    public User findOrCreateSocialUser(
            SocialUserInfo info,
            SocialType socialType
    ) {
        return userRepository.findBySocialIdAndSocialType(info.getSocialId(), socialType)
                .orElseGet(() ->userRepository.save(User.createSocialUser(info, socialType)));
    }
}
