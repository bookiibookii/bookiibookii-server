package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.book.dto.req.BookReqDTO;
import com.example.bookiibookii.domain.user.dto.req.BookshelfRequestDTO;
import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.dto.res.BookshelfResponseDTO;
import com.example.bookiibookii.domain.user.dto.res.ProfileShareTokenResponseDTO;
import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.NicknameStatus;
import com.example.bookiibookii.domain.user.exception.code.UserImageSuccessCode;
import com.example.bookiibookii.domain.user.exception.code.UserSuccessCode;
import com.example.bookiibookii.domain.user.service.BookshelfService;
import com.example.bookiibookii.domain.user.service.ProfileShareService;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.domain.user.service.UserService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerDocs{
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 10;

    private final UserService userService;
    private final UserImageS3Service userImageS3Service;
    private final BookshelfService bookshelfService;
    private final ProfileShareService profileShareService;

    // 사용자 이미지 업로드용 Presigned URL 발급
    @Override
    @PostMapping("/api/users/me/image/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForUserImage(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        PresignedUrlResponseDTO responseDTO = userImageS3Service.generatePresignedPutUrl(user.getId(), PRESIGNED_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(UserImageSuccessCode.PRESIGNED_URL_ISSUED, responseDTO);
    }

    // 닉네임 검증
    @Override
    @PostMapping("/api/users/name-validation")
    public ApiResponse<UserResponseDTO.NicknameValidationDTO> validateNickname(
            @NotBlank(message = "닉네임은 필수 입력 사항입니다.")
            @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
            @RequestParam String nickname
    ) {
        NicknameStatus status = userService.checkNicknameStatus(nickname);
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                UserResponseDTO.NicknameValidationDTO.from(status)
        );
    }

    // User 온보딩 설정
    @Override
    @PostMapping("/api/onboarding")
    public ApiResponse<Void> createUserOnboarding(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody UserRequestDTO.OnboardingReqDTO request
    ) {
        userService.createUserOnboarding(user.getId(), request);
        return ApiResponse.onSuccess(UserSuccessCode.ONBOARDING_SUCCESS, null);
    }

    // 스플래시 온보딩 스킵 (온보딩 상태 변경)
    @Override
    @PatchMapping("/api/onboarding-skip")
    public ApiResponse<Void> completeSplashOnboarding(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        userService.completeSplashOnboarding(user.getId());
        return ApiResponse.onSuccess(UserSuccessCode.ONBOARDING_SUCCESS, null);
    }

    // MyPage 조회
    @Override
    @GetMapping("/api/mypage")
    public ApiResponse<UserResponseDTO.UserProfileResDTO> getMypage(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        UserResponseDTO.UserProfileResDTO result = userService.getProfileInfo(user.getId());
        return ApiResponse.onSuccess(UserSuccessCode.GET_MYPAGE_SUCCESS, result);
    }

    // MyPage 정보 수정
    @Override
    @PatchMapping("/api/mypage")
    public ApiResponse<Void> updateMypage(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody UserRequestDTO.MypageReqDTO request
    ) {
        userService.updateMypage(user.getId(), request);
        return ApiResponse.onSuccess(UserSuccessCode.UPDATE_MYPAGE_SUCCESS, null);
    }

    // 한줄 소개 수정
    @Override
    @PatchMapping("/api/mypage/introduction")
    public ApiResponse<Void> updateIntroduction(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody UserRequestDTO.UpdateIntroductionReqDTO request
    ) {
        userService.updateIntroduction(user.getId(), request.introduction());
        return ApiResponse.onSuccess(UserSuccessCode.UPDATE_INTRODUCTION_SUCCESS, null);
    }

    // 타 유저 프로필 조회
    @Override
    @GetMapping("/api/profiles/{nickname}")
    public ApiResponse<UserResponseDTO.UserProfileResDTO> getOtherProfile(
            @PathVariable("nickname") String nickname
    ) {
        Long targetUserId = userService.findUserIdByNickname(nickname);
        UserResponseDTO.UserProfileResDTO result = userService.getProfileInfo(targetUserId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    // 나의 책장 조회
    @Override
    @GetMapping("/api/mypage/bookshelf")
    public ApiResponse<BookshelfResponseDTO.BookshelfResDTO> getBookshelf(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        BookshelfResponseDTO.BookshelfResDTO result = bookshelfService.getBookshelf(user.getId());
        return ApiResponse.onSuccess(UserSuccessCode.GET_BOOKSHELF_SUCCESS, result);
    }

    // 인생 책 등록
    @Override
    @PostMapping("/api/mypage/bookshelf/favorites")
    public ApiResponse<Void> addFavoriteBook(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody BookReqDTO.UserPickISBN request
    ) {
        bookshelfService.addFavoriteBook(user.getId(), request.isbn13());
        return ApiResponse.onSuccess(UserSuccessCode.FAVORITE_BOOK_ADD_SUCCESS, null);
    }

    // 인생 책 삭제
    @Override
    @DeleteMapping("/api/mypage/bookshelf/favorites/{userBookId}")
    public ApiResponse<Void> deleteFavoriteBook(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userBookId
    ) {
        bookshelfService.deleteFavoriteBook(user.getId(), userBookId);
        return ApiResponse.onSuccess(UserSuccessCode.FAVORITE_BOOK_DELETE_SUCCESS, null);
    }

    // 대표책 등록
    @Override
    @PostMapping("/api/mypage/bookshelf/representatives")
    public ApiResponse<Void> addRepresentativeBook(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody BookshelfRequestDTO.AddRepresentativeReqDTO request
    ) {
        bookshelfService.addRepresentativeBook(
                user.getId(), request.userBookId(), request.memberBookId()
        );
        return ApiResponse.onSuccess(UserSuccessCode.REPRESENTATIVE_BOOK_ADD_SUCCESS, null);
    }

    // 대표책 삭제
    @Override
    @DeleteMapping("/api/mypage/bookshelf/representatives/{userBookId}")
    public ApiResponse<Void> deleteRepresentativeBook(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable Long userBookId
    ) {
        bookshelfService.deleteRepresentativeBook(user.getId(), userBookId);
        return ApiResponse.onSuccess(UserSuccessCode.REPRESENTATIVE_BOOK_DELETE_SUCCESS, null);
    }

    // 대표책 순서 변경
    @Override
    @PatchMapping("/api/mypage/bookshelf/representatives/order")
    public ApiResponse<Void> reorderRepresentativeBooks(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody BookshelfRequestDTO.MoveRepresentativeReqDTO request
    ) {
        bookshelfService.reorderRepresentativeBooks(user.getId(), request.userBookId(), request.targetOrder());
        return ApiResponse.onSuccess(UserSuccessCode.REPRESENTATIVE_BOOK_REORDER_SUCCESS, null);
    }

    @Override
    @PostMapping("/api/mypage/share-token")
    public ApiResponse<ProfileShareTokenResponseDTO> createProfileShareToken(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        ProfileShareTokenResponseDTO response = profileShareService.createShareToken(user);
        return ApiResponse.onSuccess(UserSuccessCode.PROFILE_SHARE_TOKEN_CREATED, response);
    }
}
