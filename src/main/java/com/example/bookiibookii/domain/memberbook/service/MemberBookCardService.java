package com.example.bookiibookii.domain.memberbook.service;

import com.example.bookiibookii.domain.groupbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.groupbook.exception.CardImageException;
import com.example.bookiibookii.domain.groupbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.groupbook.service.CardImageS3Service;
import com.example.bookiibookii.domain.groupbook.service.CardImageValidationService;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardCreateRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardCreateResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardImageResponseDTO;
import com.example.bookiibookii.domain.memberbook.entity.CardImages;
import com.example.bookiibookii.domain.memberbook.entity.Cards;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.enums.CardType;
import com.example.bookiibookii.domain.memberbook.exception.MemberBookException;
import com.example.bookiibookii.domain.memberbook.exception.code.MemberBookErrorCode;
import com.example.bookiibookii.domain.memberbook.repository.CardImagesRepository;
import com.example.bookiibookii.domain.memberbook.repository.CardsRepository;
import com.example.bookiibookii.domain.memberbook.repository.MemberBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberBookCardService {

    private final MemberBookRepository memberBookRepository;
    private final CardsRepository cardsRepository;
    private final CardImagesRepository cardImagesRepository;
    private final CardImageS3Service cardImageS3Service;
    private final CardImageValidationService cardImageValidationService;

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

        validateS3Key(s3Key);

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

    private void validateS3Key(String s3Key) {
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
        if (page == null) {
            return;
        }

        Integer totalPages = memberBook.getBook().getTotalPages();
        if (totalPages == null || totalPages <= 0) {
            return;
        }

        Integer maxPage = cardsRepository.findTopByMemberBook_IdOrderByPageDesc(memberBook.getId())
                .map(Cards::getPage)
                .filter(p -> p != null && p > 0)
                .orElse(page);

        int rate = (maxPage * 100) / totalPages;
        memberBook.updateProgressRate(rate);
    }

    private MemberCardCreateResponseDTO buildResponse(
            Cards card,
            CardImages cardImage,
            int presignedGetUrlExpirationMinutes
    ) {
        MemberCardImageResponseDTO imageDto = null;
        if (cardImage != null) {
            imageDto = MemberCardImageResponseDTO.builder()
                    .cardImageId(cardImage.getId())
                    .s3Key(cardImage.getS3Key())
                    .presignedGetUrl(cardImageS3Service.generatePresignedGetUrl(
                            cardImage.getS3Key(), presignedGetUrlExpirationMinutes))
                    .build();
        }

        return MemberCardCreateResponseDTO.builder()
                .cardId(card.getId())
                .cardType(card.getCardType())
                .page(card.getPage())
                .memo(card.getMemo())
                .quotation(card.getQuotation())
                .cardImage(imageDto)
                .createdAt(card.getCreatedAt())
                .build();
    }
}
