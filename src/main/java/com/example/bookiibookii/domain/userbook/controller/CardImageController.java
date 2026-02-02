package com.example.bookiibookii.domain.userbook.controller;

import com.example.bookiibookii.domain.userbook.dto.res.CardCreateResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardImageResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageSuccessCode;
import com.example.bookiibookii.domain.userbook.service.CardImageS3Service;
import com.example.bookiibookii.domain.userbook.service.CardService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Validated
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardImageController implements CardImageControllerDocs {

    private final CardImageS3Service cardImageS3Service;
    private final CardService cardService;
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 10;
    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    @Override
    @PostMapping("/{cardId}/images/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrl(
            @PathVariable Long cardId
    ) {
        // 카드 존재 확인 (카드 수정 시 이미지 업로드에 사용)
        cardService.getCard(cardId);

        PresignedUrlResponseDTO responseDTO = 
                cardImageS3Service.generatePresignedPutUrl(PRESIGNED_URL_EXPIRATION_MINUTES);

        return ApiResponse.onSuccess(CardImageSuccessCode.PRESIGNED_URL_ISSUED, responseDTO);
    }

    @Override
    @GetMapping("/{cardId}")
    public ApiResponse<CardCreateResponseDTO> getCardDetail(
            @PathVariable Long cardId
    ) {
        Card card = cardService.getCardWithCardImage(cardId);
        CardImage cardImage = card.getCardImage();

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

        return ApiResponse.onSuccess(CardImageSuccessCode.CARD_FOUND, responseDTO);
    }
}
