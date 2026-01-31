package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.tag.entity.Tag;
import com.example.bookiibookii.domain.tag.enums.TagType;
import com.example.bookiibookii.domain.tag.exception.TagException;
import com.example.bookiibookii.domain.tag.exception.code.TagErrorCode;
import com.example.bookiibookii.domain.tag.repository.TagRepository;
import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserImage;
import com.example.bookiibookii.domain.user.entity.UserTag;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.enums.Status;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.UserImageException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.exception.code.UserImageErrorCode;
import com.example.bookiibookii.domain.user.repository.UserImageRepository;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.domain.user.repository.UserTagRepository;
import com.example.bookiibookii.global.auth.social.SocialUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserTagRepository userTagRepository;
    private final TagRepository tagRepository;
    private final UserImageRepository userImageRepository;
    private final UserImageValidationService userImageValidationService;
    private final UserImageS3Service userImageS3Service;

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

        if (isNicknameAvailable(request.name())) user.updateName(request.name());

        // 프로필 이미지(s3Key) 처리: 있으면 검증 후 UserImage 생성/갱신
        if (request.s3Key() != null && !request.s3Key().isBlank()) {
            saveOrUpdateUserImage(user, request.s3Key());
        }

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

        userTagRepository.deleteAllByUser(user);
        userTagRepository.saveAll(userTags);
    }

    private void saveOrUpdateUserImage(User user, String s3Key) {
        if (!userImageValidationService.isValidS3Key(s3Key)) {
            throw new UserImageException(UserImageErrorCode.INVALID_S3_KEY_FORMAT);
        }
        // s3Key 형식: image/users/{userId}/{uuid} — 소유자 검증
        long keyUserId;
        try {
            keyUserId = Long.parseLong(s3Key.split("/")[2]);
        } catch (NumberFormatException e) {
            throw new UserImageException(UserImageErrorCode.INVALID_S3_KEY_FORMAT);
        }
        if (keyUserId != user.getId()) {
            throw new UserImageException(UserImageErrorCode.S3_KEY_USER_MISMATCH);
        }
        if (!userImageS3Service.doesImageExist(s3Key)) {
            throw new UserImageException(UserImageErrorCode.IMAGE_NOT_FOUND_IN_S3);
        }
        if (userImageRepository.existsByS3KeyAndUser_IdNot(s3Key, user.getId())) {
            throw new UserImageException(UserImageErrorCode.DUPLICATE_S3_KEY);
        }

        Optional<UserImage> existingOpt = userImageRepository.findByUser_Id(user.getId());
        if (existingOpt.isPresent()) {
            UserImage existing = existingOpt.get();
            if (!existing.getS3Key().equals(s3Key)) {
                existing.updateS3Key(s3Key);
                userImageRepository.saveAndFlush(existing);
            }
        } else {
            UserImage userImage = UserImage.builder()
                    .user(user)
                    .s3Key(s3Key)
                    .build();
            try {
                userImageRepository.saveAndFlush(userImage);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                if (userImageRepository.existsByS3Key(s3Key)) {
                    throw new UserImageException(UserImageErrorCode.DUPLICATE_S3_KEY);
                }
                throw new UserImageException(UserImageErrorCode.DUPLICATE_S3_KEY);
            }
        }
    }
}   
