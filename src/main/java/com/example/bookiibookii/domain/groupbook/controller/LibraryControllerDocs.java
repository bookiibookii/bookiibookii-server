package com.example.bookiibookii.domain.groupbook.controller;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.groupbook.dto.req.LibraryBookRequestDTO;
import com.example.bookiibookii.domain.groupbook.dto.res.LibraryBookResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Library", description = "라이브러리(내 책 목록) 관련 API")
public interface LibraryControllerDocs {

    @Operation(
            summary = "서재에서 제거",
            description = """
            내 서재에서 해당 GroupBook(책)을 제거합니다. 소프트 삭제이며, 그룹·카드는 삭제되지 않습니다.
            그룹 내 다른 멤버는 그룹과 카드를 계속 조회할 수 있습니다. 본인 소유 GroupBook만 제거 가능합니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제거 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "GroupBook 없음 또는 소유자가 아님")
    })
    @DeleteMapping("/{groupBookId}")
    ApiResponse<Void> removeFromLibrary(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long groupBookId
    );

    @Operation(
            summary = "라이브러리 책 목록 조회",
            description = """
        현재 사용자의 라이브러리(GroupBook 목록)를 조회합니다.
        
        - **조회 조건**: 현재 로그인한 사용자의 GroupBook 중 삭제되지 않은(removedAt IS NULL) 목록을 반환합니다.
        - **주요 포함 정보**:
            - **유저북 ID**: `groupBookId` (서재 내 고유 식별자)
            - **도서 정보**: 책 ID, 제목, 저자, 이미지 URL
            - **호스트 정보**: 호스트 ID, 닉네임, 프로필 이미지(Presigned URL)
            - **일정 정보**: 그룹 시작일(`startDate`), **실제 독서 종료일(`endDate`)**, 그룹 독서 기간(`duration`)
            - **평가 정보**: 나의 별점(`rating`), 감상평(`comment`)
        - **참고**: `endDate`는 트래커의 실제 기록을 바탕으로 제공되며, 기록이 없는 경우 그룹 설정상의 종료일을 반환합니다.
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
