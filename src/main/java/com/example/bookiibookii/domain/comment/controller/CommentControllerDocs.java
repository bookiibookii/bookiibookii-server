package com.example.bookiibookii.domain.comment.controller;

import com.example.bookiibookii.domain.comment.dto.req.CommentCreateReqDTO;
import com.example.bookiibookii.domain.comment.dto.res.CommentCreateResDTO;
import com.example.bookiibookii.domain.comment.dto.res.CommentTreeResDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface CommentControllerDocs {

    @Operation(
            summary = "댓글 작성 api",
            description = "대댓글이 아닐 시 parentId 에 null을 입력하세요 / 대댓글일 시 부모의 commentId를 입력하세요"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "댓글 작성 실패")
    })
    @PostMapping("/groups/{groupId}/comments")
    public ApiResponse<CommentCreateResDTO> create(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody CommentCreateReqDTO req
    );

    @Operation(
            summary = "그룹의 댓글 조회 api",
            description = "대댓글 - 부모댓글 순서로 조회됩니다(시간순 정렬 포함)"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "댓글 조회 실패")
    })
    @GetMapping("/groups/{groupId}/comments")
    public ApiResponse<List<CommentTreeResDTO>> getGroupComments(
            @PathVariable Long groupId
    );
}
