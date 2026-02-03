package com.example.bookiibookii.domain.comment.controller;

import com.example.bookiibookii.domain.comment.dto.req.CardCommentReqDTO;
import com.example.bookiibookii.domain.comment.dto.res.CardCommentResDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface CardCommentControllerDocs {

    @Operation(
            summary = "카드 댓글 작성 api",
            description = "카드(cardId)에 댓글을 작성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "카드 댓글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청값이 올바르지 않음(빈 내용/길이 초과 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드를 찾을 수 없음")
    })
    @PostMapping("/cards/{cardId}/comments")
    ApiResponse<CardCommentResDTO.Create> create(
            @PathVariable Long cardId,
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody CardCommentReqDTO.Create req
    );

    @Operation(
            summary = "카드 댓글 목록 조회 api",
            description = "카드(cardId)의 댓글 목록을 시간순(오래된 댓글 -> 최신 댓글)으로 조회합니다. totalCount를 함께 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카드 댓글 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드를 찾을 수 없음")
    })
    @GetMapping("/cards/{cardId}/comments")
    ApiResponse<CardCommentResDTO.ListResponse> getCardComments(
            @PathVariable Long cardId
    );

    @Operation(
            summary = "카드 댓글 삭제 api",
            description = "카드(cardId)에 속한 특정 댓글(commentId)을 삭제합니다. 작성자만 삭제 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카드 댓글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한 없음(작성자 아님)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드 댓글을 찾을 수 없음")
    })
    @DeleteMapping("/cards/{cardId}/comments/{commentId}")
    ApiResponse<Void> delete(
            @PathVariable Long cardId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal(expression = "user") User user
    );
}
