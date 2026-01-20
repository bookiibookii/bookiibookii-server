package com.example.bookiibookii.domain.userbook.controller;

import com.example.bookiibookii.domain.userbook.dto.req.CardImageRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardImageResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.service.CardImageService;
import com.example.bookiibookii.domain.userbook.service.CardImageS3Service;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Validated
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardImageController implements CardImageControllerDocs {

    private final CardImageS3Service cardImageS3Service;
    private final CardImageService cardImageService;
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 10;
    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    /**
     * Presigned PUT URL 발급
     * POST /api/cards/{cardId}/images/presigned-url?imageCount=3
     */
    @PostMapping("/{cardId}/images/presigned-url")
    public ResponseEntity<ApiResponse<List<PresignedUrlResponseDTO>>> getPresignedPutUrls(
            @PathVariable Long cardId,
            @RequestParam(defaultValue = "1") int imageCount
    ) {
        List<CardImageS3Service.PresignedUrlResponse> presignedUrls = 
                cardImageS3Service.generatePresignedPutUrls(cardId, imageCount, PRESIGNED_URL_EXPIRATION_MINUTES);

        List<PresignedUrlResponseDTO> responseDTOs = presignedUrls.stream()
                .map(url -> PresignedUrlResponseDTO.builder()
                        .s3Key(url.s3Key())
                        .presignedUrl(url.presignedUrl())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, responseDTOs));
    }

    /**
     * 카드 이미지 DB 저장
     * POST /api/cards/{cardId}/images
     */
    @PostMapping("/{cardId}/images")
    public ResponseEntity<ApiResponse<List<CardImageResponseDTO>>> saveCardImages(
            @PathVariable Long cardId,
            @Valid @RequestBody CardImageRequestDTO request
    ) {
        // 중복 체크
        for (String s3Key : request.getS3Keys()) {
            if (cardImageService.existsByS3Key(s3Key)) {
                throw new IllegalArgumentException("이미 존재하는 S3 키입니다: " + s3Key);
            }
        }

        List<CardImage> savedImages = cardImageService.saveCardImages(cardId, request.getS3Keys());

        List<CardImageResponseDTO> responseDTOs = savedImages.stream()
                .map(image -> CardImageResponseDTO.builder()
                        .cardImageId(image.getId())
                        .s3Key(image.getS3Key())
                        .imageUrl(cardImageS3Service.generatePresignedGetUrl(
                                image.getS3Key(), 
                                PRESIGNED_GET_URL_EXPIRATION_MINUTES))
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess(GeneralSuccessCode.CREATED, responseDTOs));
    }

    /**
     * 카드 이미지 조회
     * GET /api/cards/{cardId}/images
     */
    @GetMapping("/{cardId}/images")
    public ResponseEntity<ApiResponse<List<CardImageResponseDTO>>> getCardImages(
            @PathVariable Long cardId
    ) {
        List<CardImage> cardImages = cardImageService.getCardImagesByCardId(cardId);

        List<CardImageResponseDTO> responseDTOs = cardImages.stream()
                .map(image -> CardImageResponseDTO.builder()
                        .cardImageId(image.getId())
                        .s3Key(image.getS3Key())
                        .imageUrl(cardImageS3Service.generatePresignedGetUrl(
                                image.getS3Key(), 
                                PRESIGNED_GET_URL_EXPIRATION_MINUTES))
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.onSuccess(GeneralSuccessCode.FOUND, responseDTOs));
    }
}
