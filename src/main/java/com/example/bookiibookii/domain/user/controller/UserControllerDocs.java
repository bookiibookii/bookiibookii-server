package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.book.dto.req.BookReqDTO;
import com.example.bookiibookii.domain.user.dto.req.BookshelfRequestDTO;
import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.dto.res.BookshelfResponseDTO;
import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Tag(name = "User", description = "유저 관련 API")
public interface UserControllerDocs {

    @Operation(
            summary = "사용자 이미지 업로드용 Presigned URL 발급",
            description = """
            온보딩 또는 프로필 이미지 업로드를 위한 Presigned URL을 발급합니다.
            - s3Key 형식: image/users/{userId}/{uuid}
            - 발급된 presignedPutUrl로 PUT 요청 후, 온보딩 API 등에서 s3Key를 전달해 저장합니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Presigned URL 발급 성공")
    })
    @PostMapping("/api/users/me/image/presigned-url")
    ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForUserImage(
            @AuthenticationPrincipal(expression = "user") User user
    );

    // api/users/name-validation
    @Operation(
            summary = "닉네임 검증 API",
            description = """
            닉네임의 중복 여부와 금칙어 포함 여부를 검사합니다.

            - SUCCESS : 사용 가능한 닉네임입니다. (isAvailable = true)
            - DUPLICATE : 이미 존재하는 닉네임입니다. (isAvailable = false)
            - BAD_WORD : 금칙어가 포함되어 있습니다. (isAvailable = false)
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "닉네임 검증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "닉네임 검증 실패")
    })
    ApiResponse<UserResponseDTO.NicknameValidationDTO> validateNickname(
            @NotBlank(message = "닉네임은 필수 입력 사항입니다.")
            @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
            @RequestParam String nickname);


    // api/users/onboarding
    @Operation(
            summary = "온보딩 기능 API",
            description = """
            유저의 닉네임, 초기 태그, 프로필 이미지를 저장합니다.
            - 이미지는 선택: /api/users/me/image/presigned-url 로 Presigned URL 발급 후 업로드하고, 받은 s3Key를 본 API의 s3Key에 넣어 호출합니다.
            - s3Key를 넣지 않으면 프로필 이미지는 null로 둡니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "온보딩 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "온보딩 저장 실패")
    })
    ApiResponse<Void> createUserOnboarding(@AuthenticationPrincipal User user, @Valid @RequestBody UserRequestDTO.OnboardingReqDTO request);


    // api/onboarding-skip
    @Operation(
            summary = "온보딩 스킵 상태 저장 API",
            description = """
            스플래시 온보딩 스킵 시 상태를 저장합니다.
            onboarding_status
            - NEW : 스플래시 온보딩 이전 상태
            - SPLASH_DONE : 스플래시 온보딩 완료 or 스킵 상태 -> 필수 정보 입력 페이지로 바로 이동
            - COMPLETED : 정보 입력 후 확인 버튼 클릭 완료 -> 메인 홈 화면으로 진입
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "온보딩 상태 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "온보딩 상태 저장 실패")
    })
    ApiResponse<Void> completeSplashOnboarding(@AuthenticationPrincipal User user);


    // api/mypage
    @Operation(
            summary = "마이페이지 조회 API",
            description = """
            마이페이지 조회하는 API입니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "마이페이지 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "마이페이지 조회 실패")
    })
    ApiResponse<UserResponseDTO.UserProfileResDTO> getMypage(@AuthenticationPrincipal User user);

    // api/profiles/{nickname}
    @Operation(
            summary = "타 유저 프로필 조회 API",
            description = """
            타 유저의 프로필을 조회하는 API입니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "프로필 조회 실패")
    })
    ApiResponse<UserResponseDTO.UserProfileResDTO> getOtherProfile(@PathVariable("nickname") String nickname);

    // api/mypage
    @Operation(
            summary = "마이페이지 정보 수정 API",
            description = """
            닉네임, 프로필 이미지, 자기소개, 배송지, 교환 장소를 한 번에 수정합니다.

            - 배송지·교환 장소: ToAdd(추가 목록), IdsToDelete(삭제할 ID 목록)로 전달. null이면 변경 없음.
            - 삭제 후 추가 순서로 처리되므로 교체(삭제+추가)도 한 번에 가능.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "닉네임 검증 실패")
    })
    ApiResponse<Void> updateMypage(@AuthenticationPrincipal User user, @Valid @RequestBody UserRequestDTO.MypageReqDTO request);

    @Operation(
            summary = "나의 책장 조회 API",
            description = """
            - completedBooks: 완독한 책 목록 (완독날짜, 책 제목, 작가, 장르, 별점)
            - favoriteBooks: 온보딩에서 등록한 인생 책 (최대 3개)
            - representativeBooks: 나를 대표하는 책 (최대 7개, displayOrder 순)
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "나의 책장 조회 성공")
    })
    @GetMapping("/api/mypage/bookshelf")
    ApiResponse<BookshelfResponseDTO.BookshelfResDTO> getBookshelf(@AuthenticationPrincipal User user);

    @Operation(
            summary = "인생 책 등록 API",
            description = """
            인생 책을 등록합니다. (최대 3개)
            - isbn13으로 책을 식별하며, DB에 없는 책은 자동으로 등록됩니다.
            - 이미 대표책으로만 등록된 책이면 인생책 플래그만 추가합니다.
            - 이미 인생책으로 등록된 책이면 400 오류를 반환합니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인생 책 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "3개 초과 또는 이미 등록된 책")
    })
    @PostMapping("/api/mypage/bookshelf/favorites")
    ApiResponse<Void> addFavoriteBook(@AuthenticationPrincipal User user, @Valid @RequestBody BookReqDTO.UserPickISBN request);

    @Operation(
            summary = "인생 책 삭제 API",
            description = """
            나의 책장에서 인생 책을 삭제합니다.
            - 대표책으로도 등록된 경우 대표책까지 삭제됩니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인생 책 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록된 인생 책을 찾을 수 없음")
    })
    @DeleteMapping("/api/mypage/bookshelf/favorites/{userBookId}")
    ApiResponse<Void> deleteFavoriteBook(@AuthenticationPrincipal User user, @PathVariable Long userBookId);

    @Operation(
            summary = "대표책 등록 API",
            description = """
            나를 대표하는 책을 등록합니다. (최대 7개)
            - userBookId: 인생책 목록에서 선택 시 (FavoriteBookDto.userBookId)
            - groupBookId: 완독책 목록에서 선택 시 (CompletedBookDto.groupBookId) — 별점 등록 완료 필수
            - 둘 중 하나만 전달하세요.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대표책 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "7개 초과 또는 별점 없는 완독책"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "책을 찾을 수 없음")
    })
    @PostMapping("/api/mypage/bookshelf/representatives")
    ApiResponse<Void> addRepresentativeBook(@AuthenticationPrincipal User user, @Valid @RequestBody BookshelfRequestDTO.AddRepresentativeReqDTO request);

    @Operation(
            summary = "대표책 삭제 API",
            description = """
            나를 대표하는 책을 삭제합니다.
            - 인생책이기도 한 경우 → displayOrder만 해제 (인생책 유지)
            - 대표책으로만 등록된 경우 → 행 완전 삭제
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대표책 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "등록된 대표책을 찾을 수 없음")
    })
    @DeleteMapping("/api/mypage/bookshelf/representatives/{userBookId}")
    ApiResponse<Void> deleteRepresentativeBook(@AuthenticationPrincipal User user, @PathVariable Long userBookId);

}
