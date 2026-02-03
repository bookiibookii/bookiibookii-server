package com.example.bookiibookii.domain.comment.controller;

import com.example.bookiibookii.domain.comment.dto.req.CardCommentReqDTO;
import com.example.bookiibookii.domain.comment.dto.res.CardCommentResDTO;
import com.example.bookiibookii.domain.comment.exception.code.CommentSuccessCode;
import com.example.bookiibookii.domain.comment.service.CardCommentService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CardCommentController implements CardCommentControllerDocs{

    private final CardCommentService cardCommentService;

    @PostMapping("/cards/{cardId}/comments")
    public ApiResponse<CardCommentResDTO.Create> create(
            @PathVariable Long cardId,
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody CardCommentReqDTO.Create req
    ){
        CardCommentResDTO.Create result = cardCommentService.create(cardId, user, req);
        return ApiResponse.onSuccess(CommentSuccessCode.CARD_CREATE_SUCCESS, result);
    }

    @GetMapping("/cards/{cardId}/comments")
    public ApiResponse<CardCommentResDTO.ListResponse> getCardComments(
            @PathVariable Long cardId
    ) {
        CardCommentResDTO.ListResponse result = cardCommentService.getList(cardId);
        return ApiResponse.onSuccess(CommentSuccessCode.COMMENT_FOUND_OK, result);
    }

    @DeleteMapping("/cards/{cardId}/comments/{commentId}")
    public ApiResponse<Void> delete(
            @PathVariable Long cardId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        cardCommentService.delete(cardId, commentId, user);
        return ApiResponse.onSuccess(CommentSuccessCode.DELETE_SUCCESS, null);
    }
}