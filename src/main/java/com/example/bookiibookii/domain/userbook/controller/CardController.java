package com.example.bookiibookii.domain.userbook.controller;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.userbook.dto.req.CardCreateRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardCreateResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardImageResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardListResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.exception.CardImageException;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageSuccessCode;
import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import com.example.bookiibookii.domain.userbook.service.CardImageS3Service;
import com.example.bookiibookii.domain.userbook.service.CardService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/card")
@RequiredArgsConstructor
public class CardController implements CardControllerDocs {

    private final CardService cardService;
    private final CardImageS3Service cardImageS3Service;
    private final UserBookRepository userBookRepository;
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 10;
    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    @Override
    @PostMapping("/{userBookId}/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForNewCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userBookId
    ) {
        // UserBook 존재 및 소유권 확인
        userBookRepository.findByIdAndUser_Id(userBookId, user.getId())
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.USER_BOOK_NOT_FOUND));

        PresignedUrlResponseDTO responseDTO = 
                cardImageS3Service.generatePresignedPutUrl(PRESIGNED_URL_EXPIRATION_MINUTES);

        return ApiResponse.onSuccess(CardImageSuccessCode.PRESIGNED_URL_ISSUED, responseDTO);
    }

    @Override
    @PostMapping("/{userBookId}")
    public ApiResponse<CardCreateResponseDTO> createCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userBookId,
            @Valid @RequestBody CardCreateRequestDTO request
    ) {
        // Card 생성 (소유권 검증 포함)
        Card card = cardService.createCard(
                userBookId,
                user.getId(),
                request.getPage(),
                request.getMemo(),
                request.getS3Key()
        );

        // CardImage 조회
        CardImage cardImage = cardService.getCardImage(card.getId());

        // Response DTO 생성
        CardImageResponseDTO cardImageResponseDTO = CardImageResponseDTO.builder()
                .cardImageId(cardImage.getId())
                .s3Key(cardImage.getS3Key())
                .presignedGetUrl(cardImageS3Service.generatePresignedGetUrl(
                        cardImage.getS3Key(),
                        PRESIGNED_GET_URL_EXPIRATION_MINUTES))
                .build();

        CardCreateResponseDTO responseDTO = CardCreateResponseDTO.builder()
                .cardId(card.getId())
                .page(card.getPage())
                .memo(card.getMemo())
                .cardImage(cardImageResponseDTO)
                .createdAt(card.getCreatedAt())
                .build();

        return ApiResponse.onSuccess(CardImageSuccessCode.CARD_CREATED, responseDTO);
    }

    @Override
    @GetMapping("/{userBookId}")
    public ApiResponse<CardListResponseDTO> getCards(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userBookId
    ) {
        CardService.CardsWithTitleResult result = cardService.getCardsByUserBookId(
                userBookId, 
                user.getId(),
                PRESIGNED_GET_URL_EXPIRATION_MINUTES
        );

        CardListResponseDTO responseDTO = CardListResponseDTO.builder()
                .title(result.title())
                .cards(result.cards())
                .build();

        return ApiResponse.onSuccess(CardImageSuccessCode.CARDS_FOUND, responseDTO);
    }
}
