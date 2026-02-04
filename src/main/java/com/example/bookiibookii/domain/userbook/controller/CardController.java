package com.example.bookiibookii.domain.userbook.controller;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.userbook.dto.req.CardCreateRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.req.CardUpdateRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardCreateResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardListResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageSuccessCode;
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
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 10;
    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    @Override
    @PostMapping("/{userBookId}/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForNewCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userBookId
    ) {
        PresignedUrlResponseDTO responseDTO = cardService.getPresignedPutUrlForNewCard(
                userBookId, user.getId(), PRESIGNED_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(CardImageSuccessCode.PRESIGNED_URL_ISSUED, responseDTO);
    }

    @Override
    @PostMapping("/{userBookId}")
    public ApiResponse<CardCreateResponseDTO> createCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userBookId,
            @Valid @RequestBody CardCreateRequestDTO request
    ) {
        CardCreateResponseDTO responseDTO = cardService.createCard(
                userBookId, user.getId(), request, PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(CardImageSuccessCode.CARD_CREATED, responseDTO);
    }

    @Override
    @GetMapping("/{userBookId}")
    public ApiResponse<CardListResponseDTO> getCards(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userBookId
    ) {
        CardListResponseDTO responseDTO = cardService.getCardsByUserBookId(
                userBookId, user.getId(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(CardImageSuccessCode.CARDS_FOUND, responseDTO);
    }

    @Override
    @GetMapping("/detail/{cardId}")
    public ApiResponse<CardCreateResponseDTO> getCardDetail(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long cardId
    ) {
        CardCreateResponseDTO responseDTO = cardService.getCardDetailResponseDTO(
                cardId, user.getId(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(CardImageSuccessCode.CARD_FOUND, responseDTO);
    }

    @Override
    @PatchMapping("/{cardId}")
    public ApiResponse<CardCreateResponseDTO> updateCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long cardId,
            @Valid @RequestBody CardUpdateRequestDTO request
    ) {
        CardCreateResponseDTO responseDTO = cardService.updateCardResponseDTO(
                cardId, user.getId(), request, PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(CardImageSuccessCode.CARD_UPDATED, responseDTO);
    }
}
