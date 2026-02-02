package com.example.bookiibookii.domain.userbook.controller;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.userbook.dto.req.LibraryBookRequestDTO;
import com.example.bookiibookii.domain.userbook.dto.res.LibraryBookResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@Tag(name = "Library", description = "라이브러리(내 책 목록) 관련 API")
public interface LibraryControllerDocs {

    @Operation(
            summary = "라이브러리 책 목록 조회",
            description = """
            현재 사용자의 라이브러리(UserBook 목록)를 조회합니다.
            
            - user_id = 현재 로그인 사용자 인 UserBook 목록을 반환합니다.
            - 각 항목: 그룹의 책(bookId, title, author, image), 그룹 호스트(hostId, hostProfileImageUrl), 그룹의 startDate·duration, UserBook의 rating, comment를 포함합니다.
            - hostProfileImageUrl은 presigned GET URL이며, 프로필이 없으면 null입니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/books")
    ApiResponse<List<LibraryBookResponseDTO>> getLibraryBooks(
            @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "내 서재 검색 API",
            description = "내 서재에 저장된 책을 제목, 저자, 한줄평 키워드로 검색합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/search")
    ApiResponse<List<LibraryBookResponseDTO>> searchLibraryBooks(
            @AuthenticationPrincipal(expression = "user") User user,
            @ModelAttribute LibraryBookRequestDTO.SearchDTO request
    );

}
