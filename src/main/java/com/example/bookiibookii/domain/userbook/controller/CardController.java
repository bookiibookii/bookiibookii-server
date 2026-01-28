package com.example.bookiibookii.domain.userbook.controller;

import com.example.bookiibookii.domain.userbook.dto.req.CardCreateRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardCreateResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardImageResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageSuccessCode;
import com.example.bookiibookii.domain.userbook.service.CardImageS3Service;
import com.example.bookiibookii.domain.userbook.service.CardService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/card")
@RequiredArgsConstructor
public class CardController implements CardControllerDocs {

    private final CardService cardService;
    private final CardImageS3Service cardImageS3Service;
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 10;
    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    @Override
    @PostMapping("/{userBookId}/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForNewCard(
            @PathVariable Long userBookId
    ) {
        PresignedUrlResponseDTO responseDTO = 
                cardImageS3Service.generatePresignedPutUrl(PRESIGNED_URL_EXPIRATION_MINUTES);

        return ApiResponse.onSuccess(CardImageSuccessCode.PRESIGNED_URL_ISSUED, responseDTO);
    }

    @Override
    @PostMapping("/{userBookId}")
    public ApiResponse<CardCreateResponseDTO> createCard(
            @PathVariable Long userBookId,
            @Valid @RequestBody CardCreateRequestDTO request
    ) {
        // Card 생성
        Card card = cardService.createCard(
                userBookId,
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
                .imageUrl(cardImageS3Service.generatePresignedGetUrl(
                        cardImage.getS3Key(),
                        PRESIGNED_GET_URL_EXPIRATION_MINUTES))
                .build();

        CardCreateResponseDTO responseDTO = CardCreateResponseDTO.builder()
                .cardId(card.getId())
                .page(card.getPage())
                .memo(card.getMemo())
                .cardImage(cardImageResponseDTO)
                .build();

        return ApiResponse.onSuccess(CardImageSuccessCode.CARD_CREATED, responseDTO);
    }
}
