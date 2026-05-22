package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.entity.*;
import com.example.bookiibookii.domain.user.enums.NicknameStatus;
import com.example.bookiibookii.domain.user.enums.OnboardingStatus;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.enums.Status;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.UserImageException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.exception.code.UserImageErrorCode;
import com.example.bookiibookii.domain.user.repository.*;
import com.example.bookiibookii.domain.groupbook.repository.GroupBookRepository;
import com.example.bookiibookii.domain.review.entity.MemberReview;
import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import com.example.bookiibookii.domain.review.repository.MemberReviewRepository;

import com.example.bookiibookii.global.auth.social.SocialUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy. MM. dd.");

    private final UserRepository userRepository;
    private final UserTagRepository userTagRepository;
    private final UserImageRepository userImageRepository;
    private final UserImageValidationService userImageValidationService;
    private final UserImageS3Service userImageS3Service;
    private final GroupBookRepository groupBookRepository;
    private final BadWordService badWordService;
    private final UserBookRepository userBookRepository;
    private final BookshelfService bookshelfService;
    private final MemberReviewRepository memberReviewRepository;

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
                    .orElseThrow(() -> new UserException(UserErrorCode.SOCIAL_USER_CREATE_RACE_CONDITION));
        }
    }

    @Transactional
    public NicknameStatus checkNicknameStatus(String nickname) {
        if (badWordService.containsBadWord(nickname)) {
            return NicknameStatus.BAD_WORD;
        }
        if (userRepository.existsByNickName(nickname)) {
            return NicknameStatus.DUPLICATE;
        }
        return NicknameStatus.AVAILABLE;
    }

    // 온보딩 세팅
    @Transactional
    public void createUserOnboarding(Long userId, UserRequestDTO.OnboardingReqDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        requireAvailableNickname(request.name());
        user.updateName(request.name());

        if (request.s3Key() != null && !request.s3Key().isBlank()) {
            saveOrUpdateUserImage(user, request.s3Key());
        }

        List<UserTag> userTags = request.tags().stream().map(tag -> UserTag.create(user, tag)).toList();

        bookshelfService.replaceAllFavoriteBooks(user, request.userBooks());
        user.updateIntroduction(request.introduction());
        requireValidBirth(request.birth());
        user.updateUserInform(request.gender(), request.birth());

        userTagRepository.deleteAllByUser(user);
        userTagRepository.saveAll(userTags);

        user.updateOnboardingStatus(OnboardingStatus.COMPLETED);
    }

    // 온보딩 스킵 상태로 업데이트
    @Transactional
    public void completeSplashOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));
        user.updateOnboardingStatus(OnboardingStatus.SPLASH_DONE);
    }

    private void saveOrUpdateUserImage(User user, String s3Key) {
        if (!userImageValidationService.isValidS3Key(s3Key)) {
            throw new UserImageException(UserImageErrorCode.INVALID_S3_KEY_FORMAT);
        }
        long keyUserId;
        try {
            String[] parts = s3Key.split("/");
            if (parts.length < 3) {
                throw new UserImageException(UserImageErrorCode.INVALID_S3_KEY_FORMAT);
            }
            keyUserId = Long.parseLong(parts[2]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
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
                throw e;
            }
        }
    }

    // 유저 프로필 조회
    @Transactional(readOnly = true)
    public UserResponseDTO.UserProfileResDTO getProfileInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        String profileImageUrl = null;
        if (user.getUserImage() != null) {
            profileImageUrl = userImageS3Service.generatePresignedGetUrl(
                    user.getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        }

        // 대표책 목록
        List<UserResponseDTO.UserBookDto> userBooks = userBookRepository.findRepresentativeBooks(userId).stream()
                .map(ub -> new UserResponseDTO.UserBookDto(ub.getBook().getTitle(), ub.getBook().getAuthor(), ub.getBook().getImage()))
                .toList();

        // 책 후기 개수
        long bookReviewCount = groupBookRepository.countReviewedBooksByUserId(userId);

        // 최신 후기 2개
        List<GroupBook> recentGroupBooks = groupBookRepository.findReviewedBooksByUserId(userId, PageRequest.of(0, 2));
        List<UserResponseDTO.BookReviewSummaryDto> recentBookReviews = recentGroupBooks.stream()
                .map(gb -> UserResponseDTO.BookReviewSummaryDto.builder()
                        .bookTitle(gb.getBook().getTitle())
                        .bookAuthor(gb.getBook().getAuthor())
                        .tradeType(gb.getGroup().getTradeType())
                        .rating(gb.getRating())
                        .comment(gb.getComment())
                        .reviewDate(gb.getUpdatedAt().format(DATE_FMT))
                        .build())
                .collect(Collectors.toList());

        // 받은 BOOM_UP 총 개수
        long boomUpCount = memberReviewRepository.countByTargetUserIdAndReaction(userId, MemberReviewReaction.BOOM_UP);

        // 최신 받은 후기 3개
        List<MemberReview> receivedReviews = memberReviewRepository.findLatestReceivedByUserId(userId, PageRequest.of(0, 3));
        List<UserResponseDTO.ReceivedMemberReviewDto> recentReceivedReviews = receivedReviews.stream()
                .map(mr -> {
                    User writer = mr.getWriter().getUser();
                    String writerProfileUrl = null;
                    if (writer.getUserImage() != null) {
                        writerProfileUrl = userImageS3Service.generatePresignedGetUrl(
                                writer.getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
                    }
                    return UserResponseDTO.ReceivedMemberReviewDto.builder()
                            .reviewerNickname(writer.getNickName())
                            .reviewerProfileUrl(writerProfileUrl)
                            .reaction(mr.getReaction())
                            .comment(mr.getComment())
                            .createdAt(mr.getCreatedAt().format(DATE_FMT))
                            .build();
                })
                .collect(Collectors.toList());

        return UserResponseDTO.UserProfileResDTO.builder()
                .userId(userId)
                .profileImageUrl(profileImageUrl)
                .nickname(user.getNickName())
                .introduction(user.getIntroduction())
                .userBooks(userBooks)
                .bookReviewCount((int) bookReviewCount)
                .recentBookReviews(recentBookReviews)
                .boomUpCount((int) boomUpCount)
                .recentReceivedReviews(recentReceivedReviews)
                .build();
    }

    // 한줄 소개 수정
    @Transactional
    public void updateIntroduction(Long userId, String introduction) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));
        user.updateIntroduction(introduction);
    }

    // 닉네임으로 유저 ID 찾기 (타 유저 프로필 조회용)
    public Long findUserIdByNickname(String nickname) {
        return userRepository.findByNickName(nickname)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND))
                .getId();
    }

    // 마이페이지 설정
    @Transactional
    public void updateMypage(Long userId, UserRequestDTO.MypageReqDTO request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        if (!request.nickname().equals(user.getNickName())) {
            requireAvailableNickname(request.nickname());
            user.updateName(request.nickname());
        }

        if (request.s3Key() != null && !request.s3Key().isBlank()) {
            saveOrUpdateUserImage(user, request.s3Key());
        }

        if (request.gender() != null || request.birth() != null) {
            if (request.birth() != null) {
                requireValidBirth(request.birth());
            }
            user.updateUserInform(
                    request.gender() != null ? request.gender() : user.getGender(),
                    request.birth() != null ? request.birth() : user.getBirth()
            );
        }
    }

    private void requireValidBirth(LocalDate birth) {
        if (!birth.isBefore(LocalDate.now())) {
            throw new UserException(UserErrorCode.INVALID_BIRTH_DATE);
        }
    }

    private void requireAvailableNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new UserException(UserErrorCode.INVALID_NICKNAME);
        }
        NicknameStatus status = checkNicknameStatus(nickname);
        if (status == NicknameStatus.DUPLICATE) {
            throw new UserException(UserErrorCode.NICKNAME_DUPLICATE);
        }
        if (status == NicknameStatus.BAD_WORD) {
            throw new UserException(UserErrorCode.NICKNAME_BAD_WORD);
        }
    }
}
