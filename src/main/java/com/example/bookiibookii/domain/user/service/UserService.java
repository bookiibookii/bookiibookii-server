package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.tag.entity.Tag;
import com.example.bookiibookii.domain.tag.enums.TagType;
import com.example.bookiibookii.domain.tag.exception.TagException;
import com.example.bookiibookii.domain.tag.exception.code.TagErrorCode;
import com.example.bookiibookii.domain.tag.repository.TagRepository;
import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
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
import com.example.bookiibookii.domain.userbook.dto.res.UserBookResponseDTO;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import com.example.bookiibookii.global.auth.social.SocialUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final UserTagService userTagService;
    private final UserBookRepository userBookRepository;
    private final MatchedMemberRepository matchedMemberRepository;

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

    // 마이페이지 조회
    @Transactional
    public UserResponseDTO.MypageDTO getMypageInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        // Top Tag 3개 조회
        List<TagType> targetTypes = List.of(TagType.METHOD, TagType.VIBE);
        List<UserTag> currentUserTags = userTagRepository.findByUserIdAndTagTypeIn(userId, targetTypes);
        // 누적도 -> 최신 등록 -> ID 순으로 태그 정렬 후 상위태그 추출
        List<Tag> myTopTags = userTagService.extractTopTags(currentUserTags, 3);
        List<String> topTagCodes = myTopTags.stream().map(ut -> ut.getCode()).toList();

        // TODO : 배지 조회

        // 완독 수 (로직에 따라 조건 추가 가능)
        Long completeBookCount = userBookRepository.countByUser_Id(userId);
        // 참여한 그룹 수 (타입별)
        Long relayCount = matchedMemberRepository.countByUser_IdAndGroup_GroupType(userId, GroupType.RELAY);
        Long togetherCount = matchedMemberRepository.countByUser_IdAndGroup_GroupType(userId, GroupType.TOGETHER);

        // 그룹 조회 (모집중, 진행중)
        List<Groups> myGroups = matchedMemberRepository.findMyActiveGroups(
                userId, List.of(GroupStatus.RECRUITING, GroupStatus.MATCHED)
        );
        List<GroupResponseDTO.MypageGroupDto> MyGroupList = myGroups.stream()
                .map(this::toMypageGroupDto)
                .collect(Collectors.toList());

        // 최근 읽은 책 조회 (최대 3개)
        List<UserBookResponseDTO.MypageBookDto> recentBooks = userBookRepository.findRecentBooksWithRating(
                userId,
                PageRequest.of(0, 3)
        );

        return UserResponseDTO.MypageDTO.builder()
                // TODO : 프로필 이미지 조회
                .userId(userId)
                .nickname(user.getName())
                .manner(user.getManner())
                .topTags(topTagCodes)
                .completeBook(completeBookCount.intValue())
                .relayGroup(relayCount.intValue())
                .togetherGroup(togetherCount.intValue())
                .groups(MyGroupList)
                .books(recentBooks)
                .build();
    }

    // 그룹 엔티티 -> 마이페이지용 DTO 변환 메서드
    private GroupResponseDTO.MypageGroupDto toMypageGroupDto(Groups group) {
        String genre = group.getBook().getCategory().name();
        List<String> displayTags = group.getGroupTags().stream()
                .map(gt -> gt.getTag().getCode())
                .toList();
        String author = group.getBook().getAuthor();

        return GroupResponseDTO.MypageGroupDto.builder()
                .groupId(group.getGroupId())
                .bookTitle(group.getBook().getTitle())
                .auth(author)
                .genre(genre)
                .groupStatus(group.getGroupStatus())
                .groupTags(displayTags)
                .build();
    }
}
