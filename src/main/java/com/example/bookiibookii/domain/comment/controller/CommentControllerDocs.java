package com.example.bookiibookii.domain.comment.controller;

import com.example.bookiibookii.domain.comment.dto.req.CommentCreateReqDTO;
import com.example.bookiibookii.domain.comment.dto.res.CommentCreateResDTO;
import com.example.bookiibookii.domain.comment.dto.res.CommentTreeResDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Comment", description = "댓글 관련 API")
public interface CommentControllerDocs {

    @Operation(
            summary = "댓글 작성 api",
            description = "댓글은 parentId에 null을, 답글은 부모 commentId를 입력하세요. 댓글과 답글 모두 비밀 설정이 가능합니다."
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
            description = "댓글과 답글을 시간순 트리로 조회합니다. 비밀 댓글/답글은 작성자, 대상자, 그룹 HOST에게만 노출됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "댓글 조회 실패")
    })
    @GetMapping("/groups/{groupId}/comments")
    public ApiResponse<List<CommentTreeResDTO>> getGroupComments(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "그룹 댓글 삭제 api"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "댓글 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "댓글 삭제 실패")
    })
    @DeleteMapping("/groups/{groupId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long groupId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal(expression = "user") User user
    );
}
