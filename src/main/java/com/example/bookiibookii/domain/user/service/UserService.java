package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.book.dto.req.BookReqDTO;
import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.service.BookService;
import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
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
import com.example.bookiibookii.domain.groupbook.dto.res.GroupBookResponseDTO;
import com.example.bookiibookii.domain.groupbook.repository.GroupBookQueryRepository;
import com.example.bookiibookii.domain.groupbook.repository.GroupBookRepository;
import com.example.bookiibookii.domain.location.dto.res.UserDeliveryResDTO;
import com.example.bookiibookii.domain.location.dto.res.UserExchangeResDTO;
import com.example.bookiibookii.domain.location.service.UserDeliveryService;
import com.example.bookiibookii.domain.location.service.UserExchangeService;

import com.example.bookiibookii.global.auth.social.SocialUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    private final UserRepository userRepository;
    private final UserTagRepository userTagRepository;
    private final UserImageRepository userImageRepository;
    private final UserImageValidationService userImageValidationService;
    private final UserImageS3Service userImageS3Service;
    private final GroupBookRepository groupBookRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final GroupBookQueryRepository groupBookQueryRepository;
    private final BadWordService badWordService;
    private final UserBookRepository userBookRepository;
    private final BookService bookService;
    private final UserDeliveryService userDeliveryService;
    private final UserExchangeService userExchangeService;

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
        // 금칙어 검사
        if (badWordService.containsBadWord(nickname)) {
            return NicknameStatus.BAD_WORD;
        }

        // 중복 검사
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

        // 프로필 이미지(s3Key) 처리: 있으면 검증 후 UserImage 생성/갱신
        if (request.s3Key() != null && !request.s3Key().isBlank()) {
            saveOrUpdateUserImage(user, request.s3Key());
        }

        List<UserTag> userTags = request.tags().stream().map(tag -> UserTag.create(user, tag)).toList();

        addUserBooks(user, request.userBooks(), true, true);
        user.updateIntroduction(request.introduction());
        user.updateUserInform(request.gender(), request.birth());

        userTagRepository.deleteAllByUser(user);
        userTagRepository.saveAll(userTags);

        user.updateOnboardingStatus(OnboardingStatus.COMPLETED);
    }

    private void addUserBooks(User user, List<BookReqDTO.UserPickISBN> isbnList, boolean is_favorite, boolean is_display) {
        List<String> distinctIsbns = isbnList.stream()
                .filter(Objects::nonNull)
                .map(BookReqDTO.UserPickISBN::isbn13)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        userBookRepository.deleteAllByUser(user);

        if (distinctIsbns.isEmpty()) return;

        List<UserBook> picks = distinctIsbns.stream()
                .map(bookService::getOrCreateByIsbn13)
                .map(book -> UserBook.create(user, book, is_favorite, is_display))
                .toList();

        userBookRepository.saveAll(picks);
    }

    private void replaceUserBooks(User user, List<BookReqDTO.UserPickISBN> picks, boolean is_favorite, boolean is_display) {
        List<BookReqDTO.UserPickISBN> safePicks =
                picks == null ? List.of() : picks;

        List<String> distinctIsbns = safePicks.stream()
                .map(BookReqDTO.UserPickISBN::isbn13)
                .filter(isbn -> isbn != null && !isbn.isBlank()).distinct().toList();

        if (distinctIsbns.size() > 7) {
            throw new UserException(UserErrorCode.USER_PICK_LIMIT_EXCEEDED);
        }

        List<UserBook> existingUserBooks = userBookRepository.findByUser(user);

        // 기존 bookId set
        Set<Long> existingBookIds = existingUserBooks.stream()
                .map(up -> up.getBook().getId())
                .collect(Collectors.toSet());

        // 새 book 목록
        List<Book> books = distinctIsbns.stream()
                .map(bookService::getOrCreateByIsbn13)
                .toList();

        Set<Long> newBookIds = books.stream()
                .map(Book::getId)
                .collect(Collectors.toSet());

        // 삭제 대상
        List<UserBook> toDelete = existingUserBooks.stream()
                .filter(up -> !newBookIds.contains(up.getBook().getId()))
                .toList();

        // 추가 대상
        List<UserBook> toAdd = books.stream()
                .filter(book -> !existingBookIds.contains(book.getId()))
                .map(book -> UserBook.create(user, book, is_favorite, is_display))
                .toList();

        userBookRepository.deleteAll(toDelete);
        userBookRepository.saveAll(toAdd);
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
        // s3Key 형식: image/users/{userId}/{uuid} — 소유자 검증
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
    public UserResponseDTO.UserProfileResDTO getProfileInfo(Long userId, List<GroupStatus> targetGroupStatuses) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        // 완독 수 (로직에 따라 조건 추가 가능)
        Long completeBookCount = groupBookRepository.countByUser_IdAndRemovedAtIsNull(userId);
        // 참여한 그룹 수 (타입별)
        Long relayCount = matchedMemberRepository.countByUser_IdAndGroup_GroupType(userId, GroupType.RELAY);

        // 그룹 조회 (모집중, 진행중)
        List<Groups> targetGroups = matchedMemberRepository.findMyActiveGroups(
                userId, targetGroupStatuses, RoleStatus.HOST
        );
        List<GroupResponseDTO.MypageGroupDto> groupList = targetGroups.stream()
                .map(this::toMypageGroupDto)
                .collect(Collectors.toList());

        // 최근 읽은 책 조회 (최대 3개)
        List<GroupBookResponseDTO.MypageBookDto> recentBooks = groupBookQueryRepository.findRecentBooksWithRating(
                userId,
                PageRequest.of(0, 3)
        );

        String profileImageUrl = null;
        if (user.getUserImage() != null) {
            profileImageUrl = userImageS3Service.generatePresignedGetUrl(
                    user.getUserImage().getS3Key(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        }

        // UserBook 조회
        List<UserResponseDTO.UserBookDto> userBooks = userBookRepository.findUserBooks(userId);

        List<UserDeliveryResDTO.UserDeliveryDto> deliveries = userDeliveryService.getMyDeliveries(userId);
        List<UserExchangeResDTO.UserExchangeDto> exchanges = userExchangeService.getMyExchanges(userId);

        return UserResponseDTO.UserProfileResDTO.builder()
                .userId(userId)
                .profileImageUrl(profileImageUrl)
                .nickname(user.getNickName())
                .introduction(user.getIntroduction())
                .completeBook(completeBookCount.intValue())
                .relayGroup(relayCount.intValue())
                .groups(groupList)
                .books(recentBooks)
                .userBooks(userBooks)
                .deliveries(deliveries)
                .exchanges(exchanges)
                .build();
    }

    // 그룹 엔티티 -> 마이페이지용 DTO 변환 메서드
    private GroupResponseDTO.MypageGroupDto toMypageGroupDto(Groups group) {
        String genre = group.getBook().getCategory().name();
        String author = group.getBook().getAuthor();

        return GroupResponseDTO.MypageGroupDto.builder()
                .groupId(group.getGroupId())
                .bookTitle(group.getBook().getTitle())
                .auth(author)
                .genre(genre)
                .groupStatus(group.getGroupStatus())
                .build();
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

        if (request.deliveryIdsToDelete() != null) {
            request.deliveryIdsToDelete().forEach(id -> userDeliveryService.deleteDelivery(userId, id));
        }
        if (request.deliveriesToAdd() != null) {
            request.deliveriesToAdd().forEach(req -> userDeliveryService.addDelivery(userId, req));
        }

        if (request.exchangeIdsToDelete() != null) {
            request.exchangeIdsToDelete().forEach(id -> userExchangeService.deleteExchange(userId, id));
        }
        if (request.exchangesToAdd() != null) {
            request.exchangesToAdd().forEach(req -> userExchangeService.addExchange(userId, req));
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
