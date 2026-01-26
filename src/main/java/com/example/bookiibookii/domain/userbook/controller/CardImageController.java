package com.example.bookiibookii.domain.userbook.controller;

import com.example.bookiibookii.domain.userbook.dto.req.CardImageRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardImageResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.exception.CardImageException;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageSuccessCode;
import com.example.bookiibookii.domain.userbook.service.CardImageService;
import com.example.bookiibookii.domain.userbook.service.CardImageS3Service;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Validated
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardImageController implements CardImageControllerDocs {

    private final CardImageS3Service cardImageS3Service;
    private final CardImageService cardImageService;
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 10;
    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    @Override
    @PostMapping("/{cardId}/images/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrl(
            @PathVariable Long cardId
    ) {
        PresignedUrlResponseDTO responseDTO = 
                cardImageS3Service.generatePresignedPutUrl(cardId, PRESIGNED_URL_EXPIRATION_MINUTES);

        return ApiResponse.onSuccess(CardImageSuccessCode.PRESIGNED_URL_ISSUED, responseDTO);
    }

    @Override
    @PostMapping("/{cardId}/images")
    public ApiResponse<CardImageResponseDTO> saveCardImage(
            @PathVariable Long cardId,
            @Valid @RequestBody CardImageRequestDTO request
    ) {
        CardImageService.SaveOrUpdateResult result = 
                cardImageService.saveOrUpdateCardImage(cardId, request.getS3Key());

        CardImageResponseDTO responseDTO = CardImageResponseDTO.builder()
                .cardImageId(result.cardImage().getId())
                .s3Key(result.cardImage().getS3Key())
                .imageUrl(cardImageS3Service.generatePresignedGetUrl(
                        result.cardImage().getS3Key(), 
                        PRESIGNED_GET_URL_EXPIRATION_MINUTES))
                .build();

        CardImageSuccessCode successCode = result.isCreated() 
                ? CardImageSuccessCode.CARD_IMAGE_SAVED 
                : CardImageSuccessCode.CARD_IMAGE_UPDATED;

        return ApiResponse.onSuccess(successCode, responseDTO);
    }

    @Override
    @GetMapping("/{cardId}/images")
    public ApiResponse<CardImageResponseDTO> getCardImage(
            @PathVariable Long cardId
    ) {
        CardImage cardImage = cardImageService.getCardImageByCardId(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_IMAGE_NOT_FOUND));

        CardImageResponseDTO responseDTO = CardImageResponseDTO.builder()
                .cardImageId(cardImage.getId())
                .s3Key(cardImage.getS3Key())
                .imageUrl(cardImageS3Service.generatePresignedGetUrl(
                        cardImage.getS3Key(), 
                        PRESIGNED_GET_URL_EXPIRATION_MINUTES))
                .build();

        return ApiResponse.onSuccess(CardImageSuccessCode.CARD_IMAGE_FOUND, responseDTO);
    }
}
