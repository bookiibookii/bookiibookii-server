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
import com.example.bookiibookii.domain.userbook.service.CardImageValidationService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Validated
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardImageController implements CardImageControllerDocs {

    private final CardImageS3Service cardImageS3Service;
    private final CardImageService cardImageService;
    private final CardImageValidationService cardImageValidationService;
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 10;
    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    /**
     * Presigned PUT URL 발급 API
     * POST /api/cards/{cardId}/images/presigned-url
     */
    @PostMapping("/{cardId}/images/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponseDTO>> getPresignedPutUrl(
            @PathVariable Long cardId
    ) {
        CardImageS3Service.PresignedUrlResponse presignedUrl = 
                cardImageS3Service.generatePresignedPutUrl(cardId, PRESIGNED_URL_EXPIRATION_MINUTES);

        PresignedUrlResponseDTO responseDTO = PresignedUrlResponseDTO.builder()
                .s3Key(presignedUrl.s3Key())
                .presignedUrl(presignedUrl.presignedUrl())
                .build();

        return ResponseEntity.ok(ApiResponse.onSuccess(CardImageSuccessCode.PRESIGNED_URL_ISSUED, responseDTO));
    }

    /**
     * 카드 이미지 DB 저장 또는 업데이트 API
     * POST /api/cards/{cardId}/images
     */
    @PostMapping("/{cardId}/images")
    public ResponseEntity<ApiResponse<CardImageResponseDTO>> saveCardImage(
            @PathVariable Long cardId,
            @Valid @RequestBody CardImageRequestDTO request
    ) {
        // s3Key 검증: 형식 및 cardId 일치 확인
        if (!cardImageValidationService.isValidS3Key(request.getS3Key(), cardId)) {
            throw new CardImageException(CardImageErrorCode.INVALID_S3_KEY_FORMAT);
        }

        // S3Key 중복 체크 (다른 카드에서 사용 중인지 확인)
        if (cardImageService.existsByS3Key(request.getS3Key())) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }

        CardImage savedImage = cardImageService.saveOrUpdateCardImage(cardId, request.getS3Key());

        CardImageResponseDTO responseDTO = CardImageResponseDTO.builder()
                .cardImageId(savedImage.getId())
                .s3Key(savedImage.getS3Key())
                .imageUrl(cardImageS3Service.generatePresignedGetUrl(
                        savedImage.getS3Key(), 
                        PRESIGNED_GET_URL_EXPIRATION_MINUTES))
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess(CardImageSuccessCode.CARD_IMAGE_SAVED, responseDTO));
    }

    /**
     * 카드 이미지 조회 API
     * GET /api/cards/{cardId}/images
     */
    @GetMapping("/{cardId}/images")
    public ResponseEntity<ApiResponse<CardImageResponseDTO>> getCardImage(
            @PathVariable Long cardId
    ) {
        Optional<CardImage> cardImageOpt = cardImageService.getCardImageByCardId(cardId);

        if (cardImageOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.onSuccess(CardImageSuccessCode.CARD_IMAGE_FOUND, null));
        }

        CardImage cardImage = cardImageOpt.get();
        CardImageResponseDTO responseDTO = CardImageResponseDTO.builder()
                .cardImageId(cardImage.getId())
                .s3Key(cardImage.getS3Key())
                .imageUrl(cardImageS3Service.generatePresignedGetUrl(
                        cardImage.getS3Key(), 
                        PRESIGNED_GET_URL_EXPIRATION_MINUTES))
                .build();

        return ResponseEntity.ok(ApiResponse.onSuccess(CardImageSuccessCode.CARD_IMAGE_FOUND, responseDTO));
    }
}
