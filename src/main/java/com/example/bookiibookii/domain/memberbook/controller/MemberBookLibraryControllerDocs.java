package com.example.bookiibookii.domain.memberbook.controller;

import com.example.bookiibookii.domain.memberbook.dto.req.LibraryMemberBookRequestDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.LibraryMemberBookResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "BookReview", description = "책 리뷰 및 독서카드 관련 API")
public interface MemberBookLibraryControllerDocs {

    @Operation(
            summary = "라이브러리 멤버북 목록 조회",
            description = """
        현재 사용자의 라이브러리(MemberBook 목록)를 조회합니다.

        - **조회 조건**: 현재 로그인한 사용자의 MatchedMember에 연결된 MemberBook 중 삭제되지 않은(removedAt IS NULL) 목록을 반환합니다.
        - **그룹당 2권**: 한 그룹에 참여한 멤버는 서로 다른 책을 가진 MemberBook 최대 2건이 각각 별도 항목으로 노출됩니다.
        - **주요 포함 정보**:
            - **멤버북 ID**: `memberBookId` (서재 내 고유 식별자)
            - **본인 책 여부**: `isMine` (true: 내가 가져온 책, false: 상대/호스트 책)
            - **도서 정보**: 책 ID, 제목, 저자, 이미지 URL, 전체 페이지, 장르 (MemberBook에 연결된 책)
            - **그룹 정보**: 그룹 ID, 그룹명(`groupName`)
            - **호스트 정보**: 호스트 ID, 닉네임, 프로필 이미지(Presigned URL)
            - **일정 정보**: 그룹 시작일(`startDate`), **실제 독서 종료일(`endDate`)**, 그룹 독서 기간(`duration`)
            - **진행률**: `progressRate` (0 ~ 100 정수 퍼센트)
            - **평가 정보**: BookReview 기준 별점(`rating`), 감상평(`comment`). 리뷰 미작성 시 null.
            - **완료 시점**: `completedAt`은 양측 파트너 후기가 모두 작성되어 교환독서가 종료된 시각입니다.
        - **참고**: `endDate`는 트래커의 실제 기록을 바탕으로 제공되며, 기록이 없는 경우 그룹 설정상의 종료일을 반환합니다.
        """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/memberbooks")
    ApiResponse<List<LibraryMemberBookResponseDTO>> getLibraryMemberBooks(
            @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "라이브러리 멤버북 검색",
            description = """
        현재 사용자의 라이브러리(MemberBook)에서 그룹명, 도서명, 저자명 키워드로 검색합니다.

        - **엔드포인트**: `GET /api/library/memberbooks/search?keyword=`
        - **검색 대상**: `groupName`, `title`, `author` (부분 일치, 대소문자 구분 DB 설정에 따름)
        - **검색어 미입력·공백**: 전체 멤버북 목록 조회와 동일 (`GET /api/library/memberbooks`)
        - **응답**: `LibraryMemberBookResponseDTO` 목록 (목록 조회 API와 동일 스키마, `groupName` 포함)
        """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/memberbooks/search")
    ApiResponse<List<LibraryMemberBookResponseDTO>> searchLibraryMemberBooks(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @ModelAttribute LibraryMemberBookRequestDTO.SearchDTO request
    );

    @Operation(
            summary = "서재에서 멤버북 제거",
            description = """
            내 서재에서 해당 MemberBook(책) 1건을 제거합니다. 소프트 삭제이며, 그룹·카드·상대 멤버 데이터는 삭제되지 않습니다.

            - **제거 단위**: `memberBookId` 1건 (그룹당 최대 2권이면 책마다 각각 제거)
            - **권한**: 본인 MatchedMember에 연결된 MemberBook만 제거 가능
            - **그룹 내 다른 멤버**: 해당 그룹·카드·자신의 다른 MemberBook은 계속 조회 가능
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제거 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "MemberBook 없음 또는 소유자가 아님")
    })
    @DeleteMapping("/memberbooks/{memberBookId}")
    ApiResponse<Void> removeFromLibrary(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long memberBookId
    );
}
