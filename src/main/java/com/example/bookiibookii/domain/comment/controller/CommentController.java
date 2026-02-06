package com.example.bookiibookii.domain.comment.controller;

import com.example.bookiibookii.domain.comment.dto.req.CommentCreateReqDTO;
import com.example.bookiibookii.domain.comment.dto.res.CommentCreateResDTO;
import com.example.bookiibookii.domain.comment.dto.res.CommentTreeResDTO;
import com.example.bookiibookii.domain.comment.exception.code.CommentSuccessCode;
import com.example.bookiibookii.domain.comment.service.CommentService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController implements CommentControllerDocs{

    private final CommentService commentService;

    @PostMapping("/groups/{groupId}/comments")
    public ApiResponse<CommentCreateResDTO> create(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody CommentCreateReqDTO req
    ){
        CommentCreateResDTO result = commentService.create(groupId, user, req);
        return ApiResponse.onSuccess(CommentSuccessCode.CREATE_SUCCESS, result);
    }

    @GetMapping("/groups/{groupId}/comments")
    public ApiResponse<List<CommentTreeResDTO>> getGroupComments(
            @PathVariable Long groupId
    ) {
        List<CommentTreeResDTO> result = commentService.getTree(groupId);
        return ApiResponse.onSuccess(CommentSuccessCode.COMMENT_FOUND_OK, result);
    }

    @DeleteMapping("/groups/{groupId}/comments/{commentId}")
    public ApiResponse<Void> deleteComment(
            @PathVariable Long groupId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        commentService.delete(groupId, commentId, user);
        return ApiResponse.onSuccess(CommentSuccessCode.DELETE_SUCCESS, null);
    }
}
