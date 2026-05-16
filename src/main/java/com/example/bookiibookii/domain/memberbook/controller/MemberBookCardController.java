package com.example.bookiibookii.domain.memberbook.controller;

import com.example.bookiibookii.domain.groupbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardCreateRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardUpdateRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardCreateResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardListResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardResponseDTO;
import com.example.bookiibookii.domain.memberbook.exception.code.MemberBookCardSuccessCode;
import com.example.bookiibookii.domain.memberbook.service.MemberBookCardService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/member-books")
@RequiredArgsConstructor
public class MemberBookCardController implements MemberBookCardControllerDocs {

    private final MemberBookCardService memberBookCardService;

    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 10;
    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    @Override
    @GetMapping("/group/{groupId}/cards")
    public ApiResponse<MemberCardListResponseDTO> getCardsByGroupId(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long groupId
    ) {
        MemberCardListResponseDTO response = memberBookCardService.getCardsByGroupId(
                groupId, user.getId(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(MemberBookCardSuccessCode.CARDS_FOUND, response);
    }

    @Override
    @GetMapping("/cards/detail/{cardId}")
    public ApiResponse<MemberCardResponseDTO> getCardDetail(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long cardId
    ) {
        MemberCardResponseDTO response = memberBookCardService.getCardDetail(
                cardId, user.getId(), PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(MemberBookCardSuccessCode.CARD_FOUND, response);
    }

    @Override
    @PostMapping("/{memberBookId}/cards/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForNewCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long memberBookId
    ) {
        PresignedUrlResponseDTO response = memberBookCardService.getPresignedPutUrlForNewCard(
                memberBookId, user.getId(), PRESIGNED_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(MemberBookCardSuccessCode.PRESIGNED_URL_ISSUED, response);
    }

    @Override
    @PostMapping("/{memberBookId}/cards")
    public ApiResponse<MemberCardCreateResponseDTO> createCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long memberBookId,
            @Valid @RequestBody MemberCardCreateRequestDTO request
    ) {
        MemberCardCreateResponseDTO response = memberBookCardService.createCard(
                memberBookId, user.getId(), request, PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(MemberBookCardSuccessCode.CARD_CREATED, response);
    }

    @Override
    @PatchMapping("/cards/{cardId}")
    public ApiResponse<MemberCardCreateResponseDTO> updateCard(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long cardId,
            @Valid @RequestBody MemberCardUpdateRequestDTO request
    ) {
        MemberCardCreateResponseDTO response = memberBookCardService.updateCard(
                cardId, user.getId(), request, PRESIGNED_GET_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(MemberBookCardSuccessCode.CARD_UPDATED, response);
    }

    @Override
    @DeleteMapping("/cards/{cardId}")
    public ApiResponse<Void> removeCardFromView(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long cardId
    ) {
        memberBookCardService.removeCardFromView(cardId, user.getId());
        return ApiResponse.onSuccess(MemberBookCardSuccessCode.CARD_REMOVED_FROM_VIEW, null);
    }
}
