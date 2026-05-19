package com.example.bookiibookii.domain.memberbook.service;

import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.memberbook.exception.CardImageException;
import com.example.bookiibookii.domain.memberbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardCreateRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardUpdateRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardCreateResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardImageResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardListResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardReactionCountDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardReactionToggleResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardResponseDTO;
import com.example.bookiibookii.domain.memberbook.entity.CardImages;
import com.example.bookiibookii.domain.memberbook.entity.CardReaction;
import com.example.bookiibookii.domain.memberbook.entity.Cards;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.entity.MemberCard;
import com.example.bookiibookii.domain.memberbook.enums.CardReactionType;
import com.example.bookiibookii.domain.memberbook.enums.CardType;
import com.example.bookiibookii.domain.memberbook.exception.MemberBookException;
import com.example.bookiibookii.domain.memberbook.exception.code.MemberBookErrorCode;
import com.example.bookiibookii.domain.memberbook.repository.CardImagesRepository;
import com.example.bookiibookii.domain.memberbook.repository.CardReactionRepository;
import com.example.bookiibookii.domain.memberbook.repository.CardsRepository;
import com.example.bookiibookii.domain.memberbook.repository.MemberBookRepository;
import com.example.bookiibookii.domain.memberbook.repository.MemberCardRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberBookCardService {

    private final MemberBookRepository memberBookRepository;
    private final CardsRepository cardsRepository;
    private final CardImagesRepository cardImagesRepository;
    private final MemberCardRepository memberCardRepository;
    private final CardReactionRepository cardReactionRepository;
    private final GroupsRepository groupsRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final CardImageS3Service cardImageS3Service;
    private final CardImageValidationService cardImageValidationService;
    private final UserImageS3Service userImageS3Service;

    @Transactional(readOnly = true)
    public MemberCardListResponseDTO getCardsByGroupId(Long groupId, Long userId, int presignedGetUrlExpirationMinutes) {
        groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        if (!matchedMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, userId)) {
            throw new GroupException(GroupErrorCode.FORBIDDEN_GROUP_ACCESS);
        }

        // member_card에서 현재 사용자·그룹 기준 숨김(소프트 삭제) 카드 ID를 먼저 조회
        Set<Long> hiddenCardIds = Set.copyOf(
                memberCardRepository.findHiddenCardIdsByUserIdAndGroupId(userId, groupId)
        );

        List<Cards> cards = cardsRepository.findByGroupIdWithMemberBookAndBookAndCreator(groupId);
        if (!hiddenCardIds.isEmpty()) {
            cards = cards.stream()
                    .filter(c -> !hiddenCardIds.contains(c.getId()))
                    .toList();
        }

        List<Long> cardIds = cards.stream().map(Cards::getId).toList();
        Set<Long> bookmarkedCardIds = cardIds.isEmpty()
                ? Set.of()
                : memberCardRepository.findBookmarkedCardIdsByUserIdAndCardIdIn(userId, cardIds);

        CardReactionContext reactionContext = loadCardReactionContext(userId, cardIds);

        List<MemberCardResponseDTO> cardDTOs = cards.stream()
                .map(card -> buildListItemResponse(
                        card,
                        presignedGetUrlExpirationMinutes,
                        bookmarkedCardIds.contains(card.getId()),
                        reactionContext
                ))
                .toList();

        return MemberCardListResponseDTO.builder()
                .groupId(groupId)
                .cards(cardDTOs)
                .build();
    }

    @Transactional(readOnly = true)
    public MemberCardResponseDTO getCardDetail(Long cardId, Long userId, int presignedGetUrlExpirationMinutes) {
        Cards card = getCardForDetail(cardId, userId);

        Optional<MemberCard> stateOpt = memberCardRepository.findByUserIdAndCardId(userId, cardId);
        if (stateOpt.map(MemberCard::isHidden).orElse(false)) {
            throw new MemberBookException(MemberBookErrorCode.MEMBER_CARD_NOT_FOUND);
        }

        boolean bookmarked = stateOpt.map(MemberCard::isBookmarked).orElse(false);
        CardReactionContext reactionContext = loadCardReactionContext(userId, List.of(cardId));
        return buildListItemResponse(card, presignedGetUrlExpirationMinutes, bookmarked, reactionContext);
    }

    /**
     * 독서카드 리액션 토글. 그룹 MatchedMember(본인 포함)만 가능.
     * 동일 리액션을 다시 누르면 취소됩니다.
     * 내 화면에서 숨긴 카드(hidden=true)는 리액션 토글 불가.
     */
    public MemberCardReactionToggleResponseDTO toggleReaction(
            Long cardId,
            Long userId,
            CardReactionType reaction
    ) {
        Cards card = getCardForDetail(cardId, userId);
        Long groupId = card.getMemberBook().getGroup().getGroupId();

        MatchedMember matchedMember = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.MATCHED_MEMBER_NOT_FOUND));

        memberCardRepository.findByMatchedMember_IdAndCard_Id(matchedMember.getId(), cardId)
                .filter(MemberCard::isHidden)
                .ifPresent(mc -> {
                    throw new MemberBookException(MemberBookErrorCode.MEMBER_CARD_NOT_FOUND);
                });

        Optional<CardReaction> existing = cardReactionRepository.findByMatchedMember_IdAndCard_IdAndReaction(
                matchedMember.getId(), cardId, reaction);

        if (existing.isPresent()) {
            cardReactionRepository.delete(existing.get());
            return MemberCardReactionToggleResponseDTO.builder()
                    .reaction(reaction)
                    .active(false)
                    .build();
        }

        try {
            cardReactionRepository.saveAndFlush(CardReaction.create(card, matchedMember, reaction));
            return MemberCardReactionToggleResponseDTO.builder()
                    .reaction(reaction)
                    .active(true)
                    .build();
        } catch (DataIntegrityViolationException e) {
            cardReactionRepository.findByMatchedMember_IdAndCard_IdAndReaction(
                            matchedMember.getId(), cardId, reaction)
                    .ifPresentOrElse(
                            cardReactionRepository::delete,
                            () -> {
                                throw new MemberBookException(MemberBookErrorCode.CARD_REACTION_STATE_CONFLICT);
                            }
                    );
            return MemberCardReactionToggleResponseDTO.builder()
                    .reaction(reaction)
                    .active(false)
                    .build();
        }
    }

    /**
     * 카드 북마크 토글. 조회 가능한 카드만 북마크 가능(소유자 또는 그룹 멤버).
     * MemberCard가 없으면 생성 후 bookmarked를 토글합니다.
     * 내 화면에서 숨긴 카드(hidden=true)는 북마크 토글 불가.
     *
     * @return 토글 후 북마크 여부 (true = 북마크됨, false = 북마크 해제됨)
     */
    public boolean toggleBookmark(Long cardId, Long userId) {
        Cards card = getCardForDetail(cardId, userId);
        Long groupId = card.getMemberBook().getGroup().getGroupId();

        MatchedMember matchedMember = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.MATCHED_MEMBER_NOT_FOUND));

        MemberCard state = memberCardRepository.findByMatchedMember_IdAndCard_Id(matchedMember.getId(), cardId)
                .orElseGet(() -> {
                    try {
                        return memberCardRepository.saveAndFlush(
                                MemberCard.builder()
                                        .card(card)
                                        .matchedMember(matchedMember)
                                        .bookmarked(false)
                                        .hidden(false)
                                        .build()
                        );
                    } catch (DataIntegrityViolationException e) {
                        return memberCardRepository.findByMatchedMember_IdAndCard_Id(matchedMember.getId(), cardId)
                                .orElseThrow(() -> new MemberBookException(
                                        MemberBookErrorCode.MEMBER_CARD_STATE_CONFLICT));
                    }
                });

        if (state.isHidden()) {
            throw new MemberBookException(MemberBookErrorCode.HIDDEN_CARD_CANNOT_BOOKMARK);
        }

        state.setBookmarked(!state.isBookmarked());
        return state.isBookmarked();
    }

    /**
     * 현재 사용자가 북마크한 memberBook 독서카드 목록을 최신 북마크 순으로 반환합니다.
     * 내 화면에서 숨긴 카드(hidden=true)는 제외합니다.
     */
    @Transactional(readOnly = true)
    public List<MemberCardResponseDTO> getMyBookmarkedCards(Long userId, int presignedGetUrlExpirationMinutes) {
        List<Cards> cards = memberCardRepository.findByUserIdAndBookmarkedTrueWithCardDetailsOrderByCreatedAtDesc(userId)
                .stream()
                .map(MemberCard::getCard)
                .toList();

        List<Long> cardIds = cards.stream().map(Cards::getId).toList();
        CardReactionContext reactionContext = loadCardReactionContext(userId, cardIds);

        return cards.stream()
                .map(card -> buildListItemResponse(
                        card, presignedGetUrlExpirationMinutes, true, reactionContext))
                .toList();
    }

    /**
     * 카드를 내 화면에서만 숨김 처리(소프트 삭제). Cards 엔티티는 삭제되지 않으며, 그룹 멤버는 계속 조회할 수 있습니다.
     * MemberCard가 없으면 생성 후 hidden=true로 설정합니다.
     */
    public void removeCardFromView(Long cardId, Long userId) {
        Cards card = getCardForDetail(cardId, userId);
        Long groupId = card.getMemberBook().getGroup().getGroupId();

        MatchedMember matchedMember = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.MATCHED_MEMBER_NOT_FOUND));

        MemberCard state = memberCardRepository.findByMatchedMember_IdAndCard_Id(matchedMember.getId(), cardId)
                .orElseGet(() -> {
                    try {
                        return memberCardRepository.saveAndFlush(
                                MemberCard.builder()
                                        .card(card)
                                        .matchedMember(matchedMember)
                                        .bookmarked(false)
                                        .hidden(false)
                                        .build()
                        );
                    } catch (DataIntegrityViolationException e) {
                        return memberCardRepository.findByMatchedMember_IdAndCard_Id(matchedMember.getId(), cardId)
                                .orElseThrow(() -> new MemberBookException(
                                        MemberBookErrorCode.MEMBER_CARD_STATE_CONFLICT));
                    }
                });

        if (state.isBookmarked()) {
            throw new MemberBookException(MemberBookErrorCode.BOOKMARKED_CARD_CANNOT_DELETE);
        }
        if (state.isHidden()) {
            return;
        }
        state.setHidden(true);
    }

    public PresignedUrlResponseDTO getPresignedPutUrlForNewCard(Long memberBookId, Long userId, int expirationMinutes) {
        validateMemberBookForCardCreation(memberBookId, userId);
        return cardImageS3Service.generatePresignedPutUrl(expirationMinutes);
    }

    public MemberCardCreateResponseDTO createCard(
            Long memberBookId,
            Long userId,
            MemberCardCreateRequestDTO request,
            int presignedGetUrlExpirationMinutes
    ) {
        MemberBook memberBook = validateMemberBookForCardCreation(memberBookId, userId);

        Integer page = request.getPage();
        if (page != null) {
            validatePage(page, memberBook.getBook().getTotalPages());
        }

        return switch (request.getCardType()) {
            case TEXT -> createTextCard(memberBook, request, page, presignedGetUrlExpirationMinutes);
            case IMAGE -> createImageCard(memberBook, request, page, presignedGetUrlExpirationMinutes);
        };
    }

    public MemberCardCreateResponseDTO updateCard(
            Long cardId,
            Long userId,
            MemberCardUpdateRequestDTO request,
            int presignedGetUrlExpirationMinutes
    ) {
        Cards card = getCardForOwner(cardId, userId);
        MemberBook memberBook = card.getMemberBook();

        if (memberBook.getRemovedAt() != null) {
            throw new MemberBookException(MemberBookErrorCode.MEMBER_BOOK_REMOVED);
        }

        if (request.getPage() != null) {
            validatePage(request.getPage(), memberBook.getBook().getTotalPages());
            card.updatePage(request.getPage());
        }
        if (request.getMemo() != null) {
            card.updateMemo(request.getMemo());
        }

        CardImages cardImage = card.getCardImages();

        if (card.getCardType() == CardType.TEXT) {
            if (request.getS3Key() != null && !request.getS3Key().isBlank()) {
                throw new MemberBookException(MemberBookErrorCode.S3_KEY_NOT_ALLOWED_FOR_TEXT);
            }
            if (request.getQuotation() != null) {
                if (request.getQuotation().isBlank()) {
                    throw new MemberBookException(MemberBookErrorCode.QUOTATION_REQUIRED);
                }
                card.updateQuotation(request.getQuotation().trim());
            }
        } else {
            if (request.getQuotation() != null) {
                throw new MemberBookException(MemberBookErrorCode.QUOTATION_NOT_ALLOWED_FOR_IMAGE);
            }
            if (request.getS3Key() != null && !request.getS3Key().isBlank()) {
                cardImage = updateCardImage(card, request.getS3Key());
            }
        }

        recalculateMemberBookProgressRate(memberBook);
        cardsRepository.flush();

        if (cardImage == null && card.getCardType() == CardType.IMAGE) {
            cardImage = cardImagesRepository.findByCard_Id(cardId).orElse(null);
        }

        return buildResponse(card, cardImage, presignedGetUrlExpirationMinutes);
    }

    private Cards getCardForOwner(Long cardId, Long userId) {
        return cardsRepository.findByIdAndOwnerUserId(cardId, userId)
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.MEMBER_CARD_NOT_FOUND));
    }

    private Cards getCardForDetail(Long cardId, Long userId) {
        Cards card = cardsRepository.findByIdWithDetails(cardId)
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.MEMBER_CARD_NOT_FOUND));

        MemberBook memberBook = card.getMemberBook();
        if (memberBook == null || memberBook.getMatchedMember() == null || memberBook.getGroup() == null) {
            throw new MemberBookException(MemberBookErrorCode.MEMBER_CARD_NOT_FOUND);
        }

        Long ownerUserId = memberBook.getMatchedMember().getUser().getId();
        Long groupId = memberBook.getGroup().getGroupId();

        boolean isOwner = ownerUserId.equals(userId);
        boolean isGroupMember = matchedMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, userId);

        if (!isOwner && !isGroupMember) {
            throw new MemberBookException(MemberBookErrorCode.MEMBER_CARD_NOT_FOUND);
        }

        return card;
    }

    private MemberBook validateMemberBookForCardCreation(Long memberBookId, Long userId) {
        MemberBook memberBook = memberBookRepository.findByIdAndMatchedMember_User_IdWithBook(memberBookId, userId)
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.MEMBER_BOOK_NOT_FOUND));

        if (memberBook.getRemovedAt() != null) {
            throw new MemberBookException(MemberBookErrorCode.MEMBER_BOOK_REMOVED);
        }
        return memberBook;
    }

    private MemberCardCreateResponseDTO createTextCard(
            MemberBook memberBook,
            MemberCardCreateRequestDTO request,
            Integer page,
            int presignedGetUrlExpirationMinutes
    ) {
        String quotation = request.getQuotation();
        if (quotation == null || quotation.isBlank()) {
            throw new MemberBookException(MemberBookErrorCode.QUOTATION_REQUIRED);
        }

        Cards savedCard = cardsRepository.save(
                Cards.builder()
                        .memberBook(memberBook)
                        .cardType(CardType.TEXT)
                        .page(page)
                        .memo(request.getMemo())
                        .quotation(quotation.trim())
                        .build()
        );

        updateMemberBookProgressRate(memberBook, page);
        return buildResponse(loadCardWithCreator(savedCard.getId()), null, presignedGetUrlExpirationMinutes);
    }

    private MemberCardCreateResponseDTO createImageCard(
            MemberBook memberBook,
            MemberCardCreateRequestDTO request,
            Integer page,
            int presignedGetUrlExpirationMinutes
    ) {
        String s3Key = request.getS3Key();
        if (s3Key == null || s3Key.isBlank()) {
            throw new MemberBookException(MemberBookErrorCode.S3_KEY_REQUIRED);
        }

        validateS3KeyForCreate(s3Key);

        Cards savedCard = cardsRepository.save(
                Cards.builder()
                        .memberBook(memberBook)
                        .cardType(CardType.IMAGE)
                        .page(page)
                        .memo(request.getMemo())
                        .build()
        );

        CardImages cardImage = CardImages.builder()
                .card(savedCard)
                .s3Key(s3Key)
                .build();

        try {
            cardImagesRepository.saveAndFlush(cardImage);
        } catch (DataIntegrityViolationException e) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }

        updateMemberBookProgressRate(memberBook, page);
        return buildResponse(loadCardWithCreator(savedCard.getId()), cardImage, presignedGetUrlExpirationMinutes);
    }

    private Cards loadCardWithCreator(Long cardId) {
        return cardsRepository.findByIdWithDetails(cardId)
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.MEMBER_CARD_NOT_FOUND));
    }

    private void validateS3KeyForCreate(String s3Key) {
        if (!cardImageValidationService.isValidS3Key(s3Key)) {
            throw new CardImageException(CardImageErrorCode.INVALID_S3_KEY_FORMAT);
        }
        if (!cardImageS3Service.doesImageExist(s3Key)) {
            throw new CardImageException(CardImageErrorCode.IMAGE_NOT_FOUND_IN_S3);
        }
        if (cardImagesRepository.existsByS3Key(s3Key)) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }
    }

    private CardImages updateCardImage(Cards card, String s3Key) {
        if (!cardImageValidationService.isValidS3Key(s3Key)) {
            throw new CardImageException(CardImageErrorCode.INVALID_S3_KEY_FORMAT);
        }
        if (!cardImageS3Service.doesImageExist(s3Key)) {
            throw new CardImageException(CardImageErrorCode.IMAGE_NOT_FOUND_IN_S3);
        }
        if (cardImagesRepository.existsByS3KeyAndCard_IdNot(s3Key, card.getId())) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }

        Optional<CardImages> existingImageOpt = cardImagesRepository.findByCard_Id(card.getId());

        if (existingImageOpt.isPresent()) {
            CardImages existingImage = existingImageOpt.get();
            if (existingImage.getS3Key().equals(s3Key)) {
                return existingImage;
            }
            existingImage.updateS3Key(s3Key);
            try {
                return cardImagesRepository.saveAndFlush(existingImage);
            } catch (DataIntegrityViolationException e) {
                throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
            }
        }

        CardImages newCardImage = CardImages.builder()
                .card(card)
                .s3Key(s3Key)
                .build();
        try {
            return cardImagesRepository.saveAndFlush(newCardImage);
        } catch (DataIntegrityViolationException e) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }
    }

    private void validatePage(Integer currentPage, Integer totalPages) {
        if (currentPage == null) {
            return;
        }
        if (currentPage <= 0) {
            throw new MemberBookException(MemberBookErrorCode.INVALID_PAGE_VALUE);
        }
        if (totalPages != null && currentPage > totalPages) {
            throw new MemberBookException(MemberBookErrorCode.PAGE_EXCEEDS_TOTAL);
        }
    }

    private void updateMemberBookProgressRate(MemberBook memberBook, Integer page) {
        if (page != null) {
            recalculateMemberBookProgressRate(memberBook);
        }
    }

    private void recalculateMemberBookProgressRate(MemberBook memberBook) {
        Integer totalPages = memberBook.getBook().getTotalPages();
        if (totalPages == null || totalPages <= 0) {
            return;
        }

        Integer maxPage = cardsRepository.findTopByMemberBook_IdOrderByPageDesc(memberBook.getId())
                .map(Cards::getPage)
                .filter(p -> p != null && p > 0)
                .orElse(null);

        if (maxPage == null) {
            return;
        }

        memberBook.updateCurrentPage(maxPage);
    }

    private MemberCardImageResponseDTO buildCardImageResponse(
            CardImages cardImage,
            int presignedGetUrlExpirationMinutes
    ) {
        if (cardImage == null) {
            return null;
        }
        String presignedGetUrl = null;
        try {
            presignedGetUrl = cardImageS3Service.generatePresignedGetUrl(
                    cardImage.getS3Key(), presignedGetUrlExpirationMinutes);
        } catch (Exception e) {
            log.warn("카드 이미지 Presigned URL 생성 실패. cardImageId={}", cardImage.getId(), e);
        }

        return MemberCardImageResponseDTO.builder()
                .cardImageId(cardImage.getId())
                .s3Key(cardImage.getS3Key())
                .presignedGetUrl(presignedGetUrl)
                .build();
    }

    private MemberCardResponseDTO buildListItemResponse(
            Cards card,
            int presignedGetUrlExpirationMinutes,
            boolean isBookmarked,
            CardReactionContext reactionContext
    ) {
        MemberBook memberBook = card.getMemberBook();
        String bookTitle = "";
        if (memberBook != null && memberBook.getBook() != null && memberBook.getBook().getTitle() != null) {
            bookTitle = memberBook.getBook().getTitle();
        }

        String creatorName = resolveCreatorName(memberBook);
        String creatorProfileImageUrl = resolveCreatorProfileImageUrl(memberBook, presignedGetUrlExpirationMinutes);
        CardReactionView reactionView = reactionContext.get(card.getId());

        return MemberCardResponseDTO.builder()
                .cardId(card.getId())
                .memberBookId(memberBook != null ? memberBook.getId() : null)
                .cardType(card.getCardType())
                .page(card.getPage())
                .memo(card.getMemo())
                .quotation(card.getQuotation())
                .cardImage(buildCardImageResponse(card.getCardImages(), presignedGetUrlExpirationMinutes))
                .createdAt(card.getCreatedAt())
                .bookTitle(bookTitle)
                .isMine(memberBook != null && memberBook.isMine())
                .isBookmarked(isBookmarked)
                .creatorName(creatorName)
                .creatorProfileImageUrl(creatorProfileImageUrl)
                .reactionCounts(buildReactionCounts(reactionView.counts()))
                .myReactions(reactionView.myReactions())
                .build();
    }

    private CardReactionContext loadCardReactionContext(Long userId, List<Long> cardIds) {
        if (cardIds.isEmpty()) {
            return CardReactionContext.empty();
        }

        Map<Long, Map<CardReactionType, Long>> countsByCardId = new HashMap<>();
        for (Object[] row : cardReactionRepository.countByCardIdsGroupByReaction(cardIds)) {
            Long cardId = (Long) row[0];
            CardReactionType type = (CardReactionType) row[1];
            Long count = (Long) row[2];
            countsByCardId
                    .computeIfAbsent(cardId, id -> new EnumMap<>(CardReactionType.class))
                    .put(type, count);
        }

        Map<Long, Set<CardReactionType>> myReactionsByCardId = new HashMap<>();
        for (CardReaction cardReaction : cardReactionRepository.findByUserIdAndCardIdIn(userId, cardIds)) {
            myReactionsByCardId
                    .computeIfAbsent(cardReaction.getCard().getId(), id -> EnumSet.noneOf(CardReactionType.class))
                    .add(cardReaction.getReaction());
        }

        Map<Long, CardReactionView> byCardId = new HashMap<>();
        Set<Long> allCardIds = new HashSet<>(cardIds);
        allCardIds.addAll(countsByCardId.keySet());
        allCardIds.addAll(myReactionsByCardId.keySet());

        for (Long cardId : allCardIds) {
            Map<CardReactionType, Long> counts = countsByCardId.getOrDefault(cardId, Map.of());
            List<CardReactionType> myReactions = new ArrayList<>(myReactionsByCardId.getOrDefault(cardId, Set.of()));
            byCardId.put(cardId, new CardReactionView(counts, myReactions));
        }

        return new CardReactionContext(byCardId);
    }

    private List<MemberCardReactionCountDTO> buildReactionCounts(Map<CardReactionType, Long> counts) {
        return Arrays.stream(CardReactionType.values())
                .map(type -> MemberCardReactionCountDTO.builder()
                        .reaction(type)
                        .count(counts.getOrDefault(type, 0L))
                        .build())
                .toList();
    }

    private record CardReactionView(Map<CardReactionType, Long> counts, List<CardReactionType> myReactions) {
        static CardReactionView empty() {
            return new CardReactionView(Map.of(), List.of());
        }
    }

    private record CardReactionContext(Map<Long, CardReactionView> byCardId) {
        static CardReactionContext empty() {
            return new CardReactionContext(Map.of());
        }

        CardReactionView get(Long cardId) {
            return byCardId.getOrDefault(cardId, CardReactionView.empty());
        }
    }

    private MemberCardCreateResponseDTO buildResponse(
            Cards card,
            CardImages cardImage,
            int presignedGetUrlExpirationMinutes
    ) {
        MemberBook memberBook = card.getMemberBook();
        return MemberCardCreateResponseDTO.builder()
                .cardId(card.getId())
                .cardType(card.getCardType())
                .page(card.getPage())
                .memo(card.getMemo())
                .quotation(card.getQuotation())
                .cardImage(buildCardImageResponse(cardImage, presignedGetUrlExpirationMinutes))
                .createdAt(card.getCreatedAt())
                .creatorName(resolveCreatorName(memberBook))
                .creatorProfileImageUrl(resolveCreatorProfileImageUrl(memberBook, presignedGetUrlExpirationMinutes))
                .build();
    }

    private String resolveCreatorName(MemberBook memberBook) {
        if (memberBook == null
                || memberBook.getMatchedMember() == null
                || memberBook.getMatchedMember().getUser() == null
                || memberBook.getMatchedMember().getUser().getNickName() == null) {
            return "";
        }
        return memberBook.getMatchedMember().getUser().getNickName();
    }

    private String resolveCreatorProfileImageUrl(MemberBook memberBook, int presignedGetUrlExpirationMinutes) {
        if (memberBook == null || memberBook.getMatchedMember() == null) {
            return null;
        }
        User user = memberBook.getMatchedMember().getUser();
        if (user == null || user.getUserImage() == null) {
            return null;
        }
        try {
            return userImageS3Service.generatePresignedGetUrl(
                    user.getUserImage().getS3Key(), presignedGetUrlExpirationMinutes);
        } catch (Exception e) {
            log.warn("작성자 프로필 이미지 Presigned URL 생성 실패", e);
            return null;
        }
    }
}
