package com.example.bookiibookii.domain.memberbook.controller;

import com.example.bookiibookii.domain.memberbook.dto.req.LibraryMemberBookRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.LibraryMemberBookResponseDTO;
import com.example.bookiibookii.domain.memberbook.service.MemberBookLibraryService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class MemberBookLibraryController implements MemberBookLibraryControllerDocs {

    private final MemberBookLibraryService memberBookLibraryService;

    @Override
    @GetMapping("/memberbooks")
    public ApiResponse<List<LibraryMemberBookResponseDTO>> getLibraryMemberBooks(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        List<LibraryMemberBookResponseDTO> result = memberBookLibraryService.getLibraryMemberBooks(user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND, result);
    }

    @Override
    @GetMapping("/memberbooks/search")
    public ApiResponse<List<LibraryMemberBookResponseDTO>> searchLibraryMemberBooks(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @ModelAttribute LibraryMemberBookRequestDTO.SearchDTO request
    ) {
        List<LibraryMemberBookResponseDTO> result = memberBookLibraryService.searchLibraryMemberBooks(
                user.getId(), request.getKeyword());
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND, result);
    }

    @Override
    @DeleteMapping("/memberbooks/{memberBookId}")
    public ApiResponse<Void> removeFromLibrary(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long memberBookId
    ) {
        memberBookLibraryService.removeFromLibrary(memberBookId, user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}
