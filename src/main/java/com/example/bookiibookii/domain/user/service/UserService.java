package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.tag.entity.Tag;
import com.example.bookiibookii.domain.tag.enums.TagType;
import com.example.bookiibookii.domain.tag.exception.TagException;
import com.example.bookiibookii.domain.tag.exception.code.TagErrorCode;
import com.example.bookiibookii.domain.tag.repository.TagRepository;
import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserTag;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.enums.Status;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.domain.user.repository.UserTagRepository;
import com.example.bookiibookii.global.auth.social.SocialUserInfo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserTagRepository userTagRepository;
    private final TagRepository tagRepository;

    // 소셜 유저 조회 or 생성
    public User findOrCreateSocialUser(
            SocialUserInfo info,
            SocialType socialType
    ) {
        try {
            return userRepository.findBySocialIdAndSocialType(info.getSocialId(), socialType)
                    .map(user -> {
                        if (user.getStatus() == Status.WITHDRAWN) {
                            user.reactivate();
                        }
                        return user;
                    })
                    .orElseGet(() -> userRepository.save(User.createSocialUser(info, socialType)));
        } catch (DataIntegrityViolationException e) {
            return userRepository.findBySocialIdAndSocialType(info.getSocialId(), socialType)
                    .orElseThrow(() -> new RuntimeException("소셜 유저 생성 중 동시성 오류로 사용자 조회 실패"));
        }
    }
    
    // 닉네임 검증
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByName(nickname); // 중복되면 False 반환
    }

    // 온보딩 세팅
    @Transactional
    public void createUserOnboarding(Long userId, UserRequestDTO.OnboardingReqDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));
        user.updateName(request.name());

        List<UserTag> userTags = new ArrayList<>();
        for (UserRequestDTO.TagSettingDTO tagDto : request.tags()) {
            TagType type = tagDto.type();
            List<String> codes = tagDto.value();
            List<Tag> tags = tagRepository.findByTypeAndCodeIn(type, codes);

            if (tags.size() != codes.size()) {
                throw new TagException(TagErrorCode.INVALID_TAG_CODE);
            }
            tags.forEach(tag -> userTags.add(UserTag.create(user, tag)));
        }

        userTagRepository.saveAll(userTags);
    }
}   
