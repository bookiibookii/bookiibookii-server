package com.example.bookiibookii.domain.memberbook.controller;

import com.example.bookiibookii.domain.groupbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.req.MemberCardCreateRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.MemberCardCreateResponseDTO;
import com.example.bookiibookii.domain.memberbook.exception.code.MemberBookCardSuccessCode;
import com.example.bookiibookii.domain.memberbook.service.MemberBookCardService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
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
}
