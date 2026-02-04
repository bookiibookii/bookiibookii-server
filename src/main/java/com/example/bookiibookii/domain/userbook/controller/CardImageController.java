package com.example.bookiibookii.domain.userbook.controller;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageSuccessCode;
import com.example.bookiibookii.domain.userbook.service.CardImageS3Service;
import com.example.bookiibookii.domain.userbook.service.CardService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @Override
    @PostMapping("/{cardId}/images/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrl(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long cardId
    ) {
        // 카드 소유자(UserBook 소유자)만 Presigned PUT URL 발급 가능 (그룹 멤버는 이미지 보기만 가능)
        cardService.getCardForOwner(cardId, user.getId());

        PresignedUrlResponseDTO responseDTO =
                cardImageS3Service.generatePresignedPutUrl(PRESIGNED_URL_EXPIRATION_MINUTES);

        return ApiResponse.onSuccess(CardImageSuccessCode.PRESIGNED_URL_ISSUED, responseDTO);
    }
}
