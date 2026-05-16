package com.example.bookiibookii.domain.memberbook.service;

import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.groupbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.groupbook.exception.CardImageException;
import com.example.bookiibookii.domain.groupbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.groupbook.service.CardImageS3Service;
import com.example.bookiibookii.domain.groupbook.service.CardImageValidationService;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardCreateRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardUpdateRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardCreateResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardImageResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardListResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardResponseDTO;
import com.example.bookiibookii.domain.memberbook.entity.CardImages;
import com.example.bookiibookii.domain.memberbook.entity.Cards;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.entity.MemberCard;
import com.example.bookiibookii.domain.memberbook.enums.CardType;
import com.example.bookiibookii.domain.memberbook.exception.MemberBookException;
import com.example.bookiibookii.domain.memberbook.exception.code.MemberBookErrorCode;
import com.example.bookiibookii.domain.memberbook.repository.CardImagesRepository;
import com.example.bookiibookii.domain.memberbook.repository.CardsRepository;
import com.example.bookiibookii.domain.memberbook.repository.MemberBookRepository;
import com.example.bookiibookii.domain.memberbook.repository.MemberCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberBookCardService {

    private final MemberBookRepository memberBookRepository;
    private final CardsRepository cardsRepository;
    private final CardImagesRepository cardImagesRepository;
    private final MemberCardRepository memberCardRepository;
    private final GroupsRepository groupsRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final CardImageS3Service cardImageS3Service;
    private final CardImageValidationService cardImageValidationService;

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

        List<MemberCardResponseDTO> cardDTOs = cards.stream()
                .map(card -> buildListItemResponse(
                        card,
                        presignedGetUrlExpirationMinutes,
                        bookmarkedCardIds.contains(card.getId())
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
        return buildListItemResponse(card, presignedGetUrlExpirationMinutes, bookmarked);
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
        return buildResponse(savedCard, null, presignedGetUrlExpirationMinutes);
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
        return buildResponse(savedCard, cardImage, presignedGetUrlExpirationMinutes);
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

        int rate = (maxPage * 100) / totalPages;
        memberBook.updateProgressRate(rate);
    }

    private MemberCardImageResponseDTO buildCardImageResponse(
            CardImages cardImage,
            int presignedGetUrlExpirationMinutes
    ) {
        if (cardImage == null) {
            return null;
        }
        return MemberCardImageResponseDTO.builder()
                .cardImageId(cardImage.getId())
                .s3Key(cardImage.getS3Key())
                .presignedGetUrl(cardImageS3Service.generatePresignedGetUrl(
                        cardImage.getS3Key(), presignedGetUrlExpirationMinutes))
                .build();
    }

    private MemberCardResponseDTO buildListItemResponse(
            Cards card,
            int presignedGetUrlExpirationMinutes,
            boolean isBookmarked
    ) {
        MemberBook memberBook = card.getMemberBook();
        String bookTitle = "";
        if (memberBook != null && memberBook.getBook() != null && memberBook.getBook().getTitle() != null) {
            bookTitle = memberBook.getBook().getTitle();
        }

        String creatorName = "";
        if (memberBook != null
                && memberBook.getMatchedMember() != null
                && memberBook.getMatchedMember().getUser() != null
                && memberBook.getMatchedMember().getUser().getNickName() != null) {
            creatorName = memberBook.getMatchedMember().getUser().getNickName();
        }

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
                .build();
    }

    private MemberCardCreateResponseDTO buildResponse(
            Cards card,
            CardImages cardImage,
            int presignedGetUrlExpirationMinutes
    ) {
        return MemberCardCreateResponseDTO.builder()
                .cardId(card.getId())
                .cardType(card.getCardType())
                .page(card.getPage())
                .memo(card.getMemo())
                .quotation(card.getQuotation())
                .cardImage(buildCardImageResponse(cardImage, presignedGetUrlExpirationMinutes))
                .createdAt(card.getCreatedAt())
                .build();
    }
}
