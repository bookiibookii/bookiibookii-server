package com.example.bookiibookii.domain.userbook.controller;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.userbook.dto.req.LibraryBookRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.res.LibraryBookResponseDTO;
import com.example.bookiibookii.domain.userbook.service.LibraryService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController implements LibraryControllerDocs {

    private final LibraryService libraryService;

    @Override
    @GetMapping("/books")
    public ApiResponse<List<LibraryBookResponseDTO>> getLibraryBooks(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        List<LibraryBookResponseDTO> result = libraryService.getLibraryBooks(user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND, result);
    }

    @GetMapping("/search")
    public ApiResponse<List<LibraryBookResponseDTO>> searchLibraryBooks(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @ModelAttribute LibraryBookRequestDTO.SearchDTO request
    ) {
        List<LibraryBookResponseDTO> result = libraryService.searchLibraryBooks(user.getId(), request.getKeyword());
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND, result);
    }
}
