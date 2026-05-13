package com.example.bookiibookii.domain.groupbook.controller;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.groupbook.dto.req.CardCreateRequestDTO;
import com.example.bookiibookii.domain.groupbook.dto.req.CardUpdateRequestDTO;
import com.example.bookiibookii.domain.groupbook.dto.res.CardBookmarkResponseDTO;
import com.example.bookiibookii.domain.groupbook.dto.res.CardCreateResponseDTO;
import com.example.bookiibookii.domain.groupbook.dto.res.CardListResponseDTO;
import com.example.bookiibookii.domain.groupbook.dto.res.GroupCardResponseDTO;
import com.example.bookiibookii.domain.groupbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.groupbook.exception.code.CardImageSuccessCode;
import com.example.bookiibookii.domain.groupbook.service.CardService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController implements CardControllerDocs {

    private final CardService cardService;
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 10;
    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    @Override
    @GetMapping("/bookmarks")
    public ApiResponse<List<GroupCardResponseDTO>> getMyBookmarkedCards(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        List<GroupCardResponseDTO> list = cardService.getMyBookmarkedCards(user.getId(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(CardImageSuccessCode.BOOKMARKED_CARDS_FOUND, list);
    }

    @Override
    @PatchMapping("/{cardId}/bookmark")
    public ApiResponse<CardBookmarkResponseDTO> toggleBookmark(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long cardId
    ) {
        boolean bookmarked = cardService.toggleBookmark(cardId, user.getId());
        return ApiResponse.onSuccess(CardImageSuccessCode.BOOKMARK_TOGGLED, CardBookmarkResponseDTO.builder().bookmarked(bookmarked).build());
    }

    @Override
    @PostMapping("/{groupBookId}/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForNewCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long groupBookId
    ) {
        PresignedUrlResponseDTO responseDTO = cardService.getPresignedPutUrlForNewCard(
                groupBookId, user.getId(), PRESIGNED_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(CardImageSuccessCode.PRESIGNED_URL_ISSUED, responseDTO);
    }

    @Override
    @PostMapping("/{cardId}/images/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForCardImageUpdate(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long cardId
    ) {
        PresignedUrlResponseDTO responseDTO = cardService.getPresignedPutUrlForCardImageUpdate(
                cardId, user.getId(), PRESIGNED_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(CardImageSuccessCode.PRESIGNED_URL_ISSUED, responseDTO);
    }

    @Override
    @PostMapping("/{groupBookId}")
    public ApiResponse<CardCreateResponseDTO> createCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long groupBookId,
            @Valid @RequestBody CardCreateRequestDTO request
    ) {
        CardCreateResponseDTO responseDTO = cardService.createCard(
                groupBookId, user.getId(), request, PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(CardImageSuccessCode.CARD_CREATED, responseDTO);
    }

    @Override
    @GetMapping("/group/{groupId}")
    public ApiResponse<CardListResponseDTO> getCards(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long groupId
    ) {
        CardListResponseDTO responseDTO = cardService.getCardsByGroupId(
                groupId, user.getId(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(CardImageSuccessCode.CARDS_FOUND, responseDTO);
    }

    @Override
    @GetMapping("/detail/{cardId}")
    public ApiResponse<GroupCardResponseDTO> getCardDetail(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long cardId
    ) {
        GroupCardResponseDTO responseDTO = cardService.getCardDetailResponseDTO(
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

    @Override
    @DeleteMapping("/{cardId}")
    public ApiResponse<Void> removeCardFromView(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long cardId
    ) {
        cardService.markCardAsDeleted(cardId, user.getId());
        return ApiResponse.onSuccess(CardImageSuccessCode.CARD_REMOVED_FROM_VIEW, null);
    }


}
