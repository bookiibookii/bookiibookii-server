package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.userbook.dto.CardImageUpdateResult;
import com.example.bookiibookii.domain.userbook.dto.req.CardCreateRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.req.CardUpdateRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardCreateResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardImageResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardListResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.GroupCardResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.entity.CardState;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import com.example.bookiibookii.domain.userbook.exception.CardException;
import com.example.bookiibookii.domain.userbook.exception.CardImageException;
import com.example.bookiibookii.domain.userbook.exception.code.CardErrorCode;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.domain.userbook.repository.CardImageRepository;
import com.example.bookiibookii.domain.userbook.repository.CardRepository;
import com.example.bookiibookii.domain.userbook.repository.CardStateRepository;
import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;
    private final CardStateRepository cardStateRepository;
    private final CardImageRepository cardImageRepository;
    private final CardImageValidationService cardImageValidationService;
    private final CardImageS3Service cardImageS3Service;
    private final UserBookRepository userBookRepository;
    private final GroupsRepository groupsRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final UserRepository userRepository;
    private final TrackerRepository trackerRepository;

    // ========== 컨트롤러에서 직접 호출 (DTO 반환) ==========

    /**
     * Presigned PUT URL 발급. UserBook 존재 및 소유권 검증 후 DTO 반환.
     */
    public PresignedUrlResponseDTO getPresignedPutUrlForNewCard(Long userBookId, Long userId, int presignedPutUrlExpirationMinutes) {
        userBookRepository.findByIdAndUser_Id(userBookId, userId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.USER_BOOK_NOT_FOUND));
        return cardImageS3Service.generatePresignedPutUrl(presignedPutUrlExpirationMinutes);
    }

    /**
     * 카드 이미지 수정용 Presigned PUT URL 발급. 카드 소유자(UserBook 소유자)만 발급 가능.
     */
    public PresignedUrlResponseDTO getPresignedPutUrlForCardImageUpdate(Long cardId, Long userId, int presignedPutUrlExpirationMinutes) {
        getCardForOwner(cardId, userId);
        return cardImageS3Service.generatePresignedPutUrl(presignedPutUrlExpirationMinutes);
    }

    /**
     * 독서카드를 생성하고 응답 DTO를 반환합니다.
     */
    @Transactional
    public CardCreateResponseDTO createCard(Long userBookId, Long userId,
                                            CardCreateRequestDTO request, int presignedGetUrlExpirationMinutes) {
        String s3Key = request.getS3Key();
        Integer page = request.getPage();
        String memo = request.getMemo();


        if (!cardImageValidationService.isValidS3Key(s3Key)) {
            throw new CardImageException(CardImageErrorCode.INVALID_S3_KEY_FORMAT);
        }
        if (!cardImageS3Service.doesImageExist(s3Key)) {
            throw new CardImageException(CardImageErrorCode.IMAGE_NOT_FOUND_IN_S3);
        }

        UserBook userBook = userBookRepository.findByIdAndUser_Id(userBookId, userId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.USER_BOOK_NOT_FOUND));

        if (page != null) {
            Integer totalPages = userBook.getBook().getTotalPages();
            validatePage(page, totalPages);
        }

        if (cardImageRepository.existsByS3Key(s3Key)) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }

        Card card = Card.builder()
                .userBook(userBook)
                .group(userBook.getGroup())
                .page(page)
                .memo(memo)
                .build();

        Card savedCard = cardRepository.save(card);

        updateProgressRate(userId, savedCard.getGroup());

        CardImage cardImage = CardImage.builder()
                .card(savedCard)
                .s3Key(s3Key)
                .build();

        try {
            cardImageRepository.saveAndFlush(cardImage);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }

        return buildCardCreateResponseDTO(savedCard, cardImage, presignedGetUrlExpirationMinutes);
    }

    /**
     * 그룹에 속한 전체 멤버의 독서카드 목록을 한 번에 조회합니다.
     * 그룹 멤버만 조회 가능하며, 생성일 기준 오름차순으로 반환합니다.
     */
    public CardListResponseDTO getCardsByGroupId(Long groupId, Long userId, int presignedGetUrlExpirationMinutes) {
        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        if (!matchedMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, userId)) {
            throw new GroupException(GroupErrorCode.FORBIDDEN_GROUP_ACCESS);
        }

        Book book = group.getBook();
        String bookTitle = (book != null && book.getTitle() != null) ? book.getTitle() : "";

        List<Card> cards = cardRepository.findByGroup_GroupIdWithCardImageAndUserBookAndGroupAndBook(groupId);
        Set<Long> hiddenCardIds = Set.copyOf(cardStateRepository.findHiddenCardIdsByUserId(userId));
        cards = cards.stream()
                .filter(c -> !hiddenCardIds.contains(c.getId()))
                .toList();

        List<Long> cardIds = cards.stream().map(Card::getId).toList();
        Set<Long> bookmarkedCardIds = cardIds.isEmpty()
                ? Set.of()
                : cardStateRepository.findBookmarkedCardIdsByUserIdAndCardIdIn(userId, cardIds);

        List<GroupCardResponseDTO> cardDTOs = cards.stream()
                .map(card -> buildGroupCardResponseDTO(card, card.getCardImage(), presignedGetUrlExpirationMinutes, bookTitle, bookmarkedCardIds.contains(card.getId()), getCreatorNameSafely(card)))
                .toList();

        var trackerOpt = trackerRepository.findByGroupId(groupId);
        CardListResponseDTO.CurrentBookOwnerDto currentBookOwner = trackerOpt
                .filter(t -> t.getBookOwner() != null && t.getBookOwner().getUser() != null)

                .map(t -> CardListResponseDTO.CurrentBookOwnerDto.builder()
                        .matchedMemberId(t.getBookOwner().getId())
                        .nickname(t.getBookOwner().getUser().getNickName() != null ? t.getBookOwner().getUser().getNickName() : "")
                        .build())
                .orElse(null);

        String myComment = null;
        String partnerComment = null;
        if (group.getGroupType() == GroupType.RELAY) {
            myComment = userBookRepository.findByUser_IdAndGroup_GroupId(userId, groupId)
                    .map(ub -> ub.getComment())
                    .orElse(null);
            if (trackerOpt.isPresent()
                    && trackerOpt.get().getBookOwner() != null
                    && trackerOpt.get().getBookOwner().getUser() != null
                    && !trackerOpt.get().getBookOwner().getUser().getId().equals(userId)) {
                Long partnerUserId = trackerOpt.get().getBookOwner().getUser().getId();
                partnerComment = userBookRepository.findByUser_IdAndGroup_GroupId(partnerUserId, groupId)
                        .map(ub -> ub.getComment())
                        .orElse(null);
            }
        }

        return CardListResponseDTO.builder()
                .groupId(groupId)
                .currentBookOwner(currentBookOwner)
                .myComment(myComment)
                .partnerComment(partnerComment)
                .cards(cardDTOs)
                .build();
    }

    /**
     * 카드 상세 조회. 카드 소유자 또는 그룹 멤버만 조회 가능하며, 응답 DTO를 반환합니다.
     */
    public GroupCardResponseDTO getCardDetailResponseDTO(Long cardId, Long userId, int presignedGetUrlExpirationMinutes) {
        Card card = getCardDetail(cardId, userId);
        var stateOpt = cardStateRepository.findByUser_IdAndCard_Id(userId, cardId);
        if (stateOpt.map(CardState::isHidden).orElse(false)) {
            throw new CardImageException(CardImageErrorCode.CARD_NOT_FOUND);
        }
        CardImage cardImage = card.getCardImage();
        String bookTitle = getBookTitleSafely(card);
        boolean bookmarked = stateOpt.map(CardState::isBookmarked).orElse(false);
        return buildGroupCardResponseDTO(card, cardImage, presignedGetUrlExpirationMinutes, bookTitle, bookmarked, getCreatorNameSafely(card));
    }

    /**
     * 독서카드를 수정하고 응답 DTO를 반환합니다.
     */
    @Transactional
    public CardCreateResponseDTO updateCardResponseDTO(Long cardId, Long userId, CardUpdateRequestDTO request, int presignedGetUrlExpirationMinutes) {
        Card card = updateCard(cardId, userId, request.getPage(), request.getMemo(), request.getS3Key());
        CardImage cardImage = card.getCardImage();
        if (cardImage == null) {
            cardImage = getCardImage(cardId);
        }
        return buildCardCreateResponseDTO(card, cardImage, presignedGetUrlExpirationMinutes);
    }

    /**
     * 카드를 "내 화면에서만 숨김" 처리. 그룹/카드는 삭제되지 않고, 해당 사용자만 목록·상세에서 제외.
     * 카드 소유자 또는 그룹 멤버만 호출 가능. 이미 삭제 처리된 경우 무시(멱등).
     */
    @Transactional
    public void markCardAsDeleted(Long cardId, Long userId) {
        Card card = getCardDetail(cardId, userId);
        CardState state = cardStateRepository.findByUser_IdAndCard_Id(userId, cardId)
                .orElseGet(() -> {
                    try {
                        return cardStateRepository.saveAndFlush(CardState.builder()
                                .user(userRepository.getReferenceById(userId))
                                .card(card)
                                .bookmarked(false)
                                .hidden(false)
                                .build());
                    } catch (DataIntegrityViolationException e) {
                        return cardStateRepository.findByUser_IdAndCard_Id(userId, cardId)
                                .orElseThrow(() -> new CardException(CardErrorCode.CARD_NOT_FOUND));
                    }
                });
        if (state.isBookmarked()) {
            throw new CardException(CardErrorCode.BOOKMARKED_CARD_CANNOT_DELETE);
        }
        if (state.isHidden()) {
            return;
        }
        state.setHidden(true);
    }

    /**
     * 카드 소유자(UserBook 소유자)만 접근 가능한 조회. 카드 이미지 수정용 Presigned URL 발급 등에서 사용.
     */
    public Card getCardForOwner(Long cardId, Long userId) {
        Card card = cardRepository.findByIdWithCardImageAndUserBookAndBook(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));

        if (!card.getUserBook().getUser().getId().equals(userId)) {
            throw new CardImageException(CardImageErrorCode.CARD_NOT_FOUND);
        }

        return card;
    }

    // ========== 내부 전용 (private) ==========

    private Card updateCard(Long cardId, Long userId, Integer page, String memo, String s3Key) {
        Card card = cardRepository.findByIdWithUserBookAndGroup(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));


        if (page != null) {
            Integer totalPages = card.getUserBook().getBook().getTotalPages();
            validatePage(page, totalPages);
            card.updatePage(page);
        }
        if (memo != null) {
            card.updateMemo(memo);
        }
        if (s3Key != null && !s3Key.isBlank()) {
            updateCardImage(cardId, s3Key);
        }

        updateProgressRate(userId, card.getGroup());

        cardRepository.flush();
        return cardRepository.findByIdWithCardImage(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));
    }

    private CardImageUpdateResult updateCardImage(Long cardId, String s3Key) {
        if (!cardImageValidationService.isValidS3Key(s3Key)) {
            throw new CardImageException(CardImageErrorCode.INVALID_S3_KEY_FORMAT);
        }
        if (!cardImageS3Service.doesImageExist(s3Key)) {
            throw new CardImageException(CardImageErrorCode.IMAGE_NOT_FOUND_IN_S3);
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));

        if (cardImageRepository.existsByS3KeyAndCard_IdNot(s3Key, cardId)) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }

        Optional<CardImage> existingImageOpt = cardImageRepository.findByCard_Id(cardId);

        if (existingImageOpt.isPresent()) {
            CardImage existingImage = existingImageOpt.get();
            if (existingImage.getS3Key().equals(s3Key)) {
                return new CardImageUpdateResult(card, existingImage, false);
            }
            existingImage.updateS3Key(s3Key);
            try {
                CardImage updatedImage = cardImageRepository.saveAndFlush(existingImage);
                return new CardImageUpdateResult(card, updatedImage, false);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                return handleDuplicateS3KeyException(e, s3Key, cardId, card);
            }
        }

        CardImage newCardImage = CardImage.builder()
                .card(card)
                .s3Key(s3Key)
                .build();

        try {
            CardImage savedImage = cardImageRepository.saveAndFlush(newCardImage);
            return new CardImageUpdateResult(card, savedImage, true);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return handleDuplicateS3KeyException(e, s3Key, cardId, card);
        }
    }

    /** @return race condition 재시도 성공 시 결과, 아니면 예외 발생 */
    private CardImageUpdateResult handleDuplicateS3KeyException(org.springframework.dao.DataIntegrityViolationException e, String s3Key, Long cardId, Card card) {
        Throwable rootCause = e.getRootCause();
        String errorMessage = rootCause instanceof java.sql.SQLException
                ? (rootCause.getMessage() != null ? rootCause.getMessage().toLowerCase() : "")
                : (rootCause != null && rootCause.getMessage() != null ? rootCause.getMessage().toLowerCase() : (e.getMessage() != null ? e.getMessage().toLowerCase() : ""));

        if (errorMessage.contains("s3_key") || errorMessage.contains("'s3_key'")) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }
        if (cardImageRepository.existsByS3Key(s3Key)) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }

        Optional<CardImage> raceConditionImage = cardImageRepository.findByCard_Id(cardId);
        if (raceConditionImage.isPresent()) {
            CardImage existingImage = raceConditionImage.get();
            existingImage.updateS3Key(s3Key);
            try {
                CardImage updatedImage = cardImageRepository.saveAndFlush(existingImage);
                return new CardImageUpdateResult(card, updatedImage, false);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
            }
        }
        throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
    }

    private Card getCard(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));
    }

    private CardImage getCardImage(Long cardId) {
        return cardImageRepository.findByCard_Id(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_IMAGE_NOT_FOUND));
    }

    private Card getCardWithCardImage(Long cardId) {
        return cardRepository.findByIdWithCardImage(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));
    }

    private Card getCardWithCardImageAndBook(Long cardId) {
        return cardRepository.findByIdWithCardImageAndUserBookAndBook(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));
    }

    private Card getCardDetail(Long cardId, Long userId) {
        Card card = cardRepository.findByIdWithCardImageAndUserBookAndBook(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));

        boolean isOwner = card.getUserBook().getUser().getId().equals(userId);
        boolean isGroupMember = false;
        if (card.getGroup() != null) {
            Long groupId = card.getGroup().getGroupId();
            isGroupMember = matchedMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, userId);
        }
        if (!isOwner && !isGroupMember) {
            throw new CardImageException(CardImageErrorCode.CARD_NOT_FOUND);
        }

        return card;
    }

    private CardCreateResponseDTO buildCardCreateResponseDTO(Card card, CardImage cardImage, int presignedGetUrlExpirationMinutes) {
        CardImageResponseDTO cardImageResponseDTO = CardImageResponseDTO.builder()
                .cardImageId(cardImage.getId())
                .s3Key(cardImage.getS3Key())
                .presignedGetUrl(cardImageS3Service.generatePresignedGetUrl(cardImage.getS3Key(), presignedGetUrlExpirationMinutes))
                .build();
        return CardCreateResponseDTO.builder()
                .cardId(card.getId())
                .page(card.getPage())
                .memo(card.getMemo())
                .cardImage(cardImageResponseDTO)
                .createdAt(card.getCreatedAt())
                .build();
    }

    private GroupCardResponseDTO buildGroupCardResponseDTO(Card card, CardImage cardImage, int presignedGetUrlExpirationMinutes, String bookTitle, boolean isBookmarked, String creatorName) {
        if (cardImage == null) {
            throw new CardImageException(CardImageErrorCode.CARD_IMAGE_NOT_FOUND);
        }
        CardImageResponseDTO imageDto = CardImageResponseDTO.builder()
                .cardImageId(cardImage.getId())
                .s3Key(cardImage.getS3Key())
                .presignedGetUrl(cardImageS3Service.generatePresignedGetUrl(cardImage.getS3Key(), presignedGetUrlExpirationMinutes))
                .build();
        return GroupCardResponseDTO.builder()
                .cardId(card.getId())
                .page(card.getPage())
                .memo(card.getMemo())
                .cardImage(imageDto)
                .createdAt(card.getCreatedAt())
                .bookTitle(bookTitle)
                .isBookmarked(isBookmarked)
                .creatorName(creatorName)
                .build();
    }

    // 북마크

    /**
     * 카드 북마크 토글. 조회 가능한 카드만 북마크 가능(소유자 또는 그룹 멤버).
     * @return 토글 후 북마크 여부 (true = 북마크됨, false = 북마크 해제됨)
     */
    @Transactional
    public boolean toggleBookmark(Long cardId, Long userId) {
        Card card = getCardDetail(cardId, userId);
        CardState state = cardStateRepository.findByUser_IdAndCard_Id(userId, cardId)
                .orElseGet(() -> {
                    try {
                        return cardStateRepository.saveAndFlush(CardState.builder()
                                .user(userRepository.getReferenceById(userId))
                                .card(card)
                                .bookmarked(false)
                                .hidden(false)
                                .build());
                    } catch (DataIntegrityViolationException e) {
                        return cardStateRepository.findByUser_IdAndCard_Id(userId, cardId)
                                .orElseThrow(() -> new CardException(CardErrorCode.CARD_STATE_CONFLICT));
                    }
                });
        state.setBookmarked(!state.isBookmarked());
        return state.isBookmarked();
    }

    /**
     * 현재 사용자가 북마크한 독서카드 목록을 반환합니다.
     */
    public List<GroupCardResponseDTO> getMyBookmarkedCards(Long userId, int presignedGetUrlExpirationMinutes) {
        List<CardState> states = cardStateRepository.findByUser_IdAndBookmarkedTrueWithCardAndImageAndBookOrderByCreatedAtDesc(userId);
        return states.stream()
                .map(cs -> {
                    Card card = cs.getCard();
                    String bookTitle = getBookTitleSafely(card);
                    return buildGroupCardResponseDTO(card, card.getCardImage(), presignedGetUrlExpirationMinutes, bookTitle, true, getCreatorNameSafely(card));
                })
                .toList();
    }

    /**
     * 카드 작성자 이름을 안전하게 조회합니다.
     */
    private String getCreatorNameSafely(Card card) {
        if (card == null || card.getUserBook() == null || card.getUserBook().getUser() == null) {
            return "";
        }
        String name = card.getUserBook().getUser().getNickName();
        return name != null ? name : "";
    }

    /**
     * 카드의 그룹·책 정보에서 책 제목을 안전하게 조회합니다.
     * 그룹/책이 null이거나 삭제된 경우 빈 문자열을 반환합니다.
     */
    private String getBookTitleSafely(Card card) {
        if (card == null || card.getUserBook() == null) {
            return "";
        }
        var group = card.getUserBook().getGroup();
        if (group == null || group.getBook() == null) {
            return "";
        }
        String title = group.getBook().getTitle();
        return title != null ? title : "";
    }
    private void validatePage(Integer currentPage, Integer totalPages) {
        if (currentPage != null && totalPages != null) {
            if (currentPage > totalPages) {
                throw new CardException(CardErrorCode.PAGE_EXCEEDS_TOTAL);
            }
            if (currentPage <= 0) {
                throw new CardException(CardErrorCode.INVALID_PAGE_VALUE);
            }
        }
    }

    /**
     * 사용자의 최신 페이지를 바탕으로 그룹 내 독서율을 업데이트합니다.
     */
    private void updateProgressRate(Long userId, Groups group) {
        if (group == null) return;

        // 1. 해당 유저의 MatchedMember 찾기
        matchedMemberRepository.findByGroup_GroupIdAndUser_Id(group.getGroupId(), userId)
                .ifPresent(mm -> {
                    // 2. 현재 CardService에 있는 로직을 활용해 독서율 계산
                    int newRate = calculateUserReadingRate(userId, group);

                    // 3. MatchedMember 엔티티의 필드 업데이트 (필드명은 예시입니다)
                    mm.updateReadingRate(newRate);

                });
    }

    // 앞서 만든 계산 로직을 CardService로 가져오기
    private int calculateUserReadingRate(Long userId, Groups group) {
        return cardRepository.findTopByUserBook_User_IdAndGroupOrderByPageDesc(userId, group)
                .map(card -> {
                    UserBook ub = card.getUserBook();
                    if (ub == null || ub.getBook() == null) return 0;

                    Integer totalPages = ub.getBook().getTotalPages();
                    Integer currentPage = card.getPage();

                    if (totalPages == null || totalPages <= 0) return 0;

                    int rate = (currentPage*100)/totalPages;
                    return Math.min(100, Math.max(0,rate));

                })
                .orElse(0);
    }

}
