package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.user.dto.res.ProfileShareTokenResponseDTO;
import com.example.bookiibookii.domain.user.dto.res.PublicProfileResponseDTO;
import com.example.bookiibookii.domain.user.entity.ProfileShareToken;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserBook;
import com.example.bookiibookii.domain.user.enums.Status;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.ProfileShareTokenRepository;
import com.example.bookiibookii.domain.user.repository.UserBookRepository;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.global.config.ShareWebProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileShareService {

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    private final ProfileShareTokenRepository profileShareTokenRepository;
    private final UserRepository userRepository;
    private final UserBookRepository userBookRepository;
    private final BookReviewRepository bookReviewRepository;
    private final UserImageS3Service userImageS3Service;
    private final ShareWebProperties shareWebProperties;

    @Transactional
    public ProfileShareTokenResponseDTO createShareToken(User user) {
        validateShareable(user);
        revokeActiveTokensForUser(user.getId());

        ProfileShareToken shareToken = profileShareTokenRepository.save(
                ProfileShareToken.create(userRepository.getReferenceById(user.getId()))
        );

        return ProfileShareTokenResponseDTO.builder()
                .shareToken(shareToken.getToken())
                .shareUrl(buildShareUrl(shareToken.getToken()))
                .build();
    }

    @Transactional(readOnly = true)
    public PublicProfileResponseDTO getPublicProfile(String token) {
        ProfileShareToken shareToken = profileShareTokenRepository.findActiveByTokenWithUserDetails(token)
                .orElseThrow(() -> new UserException(UserErrorCode.PROFILE_SHARE_TOKEN_NOT_FOUND));

        User user = shareToken.getUser();
        validateShareableForPublic(user);

        return toPublicResponse(user);
    }

    @Transactional
    public void revokeActiveTokensForUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<ProfileShareToken> activeTokens = profileShareTokenRepository.findAllActiveByUserId(userId);
        activeTokens.forEach(token -> token.revoke(now));
    }

    private void validateShareableForPublic(User user) {
        try {
            validateShareable(user);
        } catch (UserException e) {
            if (e.getCode() == UserErrorCode.PROFILE_NOT_SHAREABLE) {
                throw new UserException(UserErrorCode.PROFILE_SHARE_TOKEN_NOT_FOUND);
            }
            throw e;
        }
    }

    private void validateShareable(User user) {
        if (user.getStatus() != Status.ACTIVE) {
            throw new UserException(UserErrorCode.PROFILE_NOT_SHAREABLE);
        }
    }

    private PublicProfileResponseDTO toPublicResponse(User user) {
        String profileImageUrl = null;
        if (user.getUserImage() != null) {
            try {
                profileImageUrl = userImageS3Service.generatePresignedGetUrl(
                        user.getUserImage().getS3Key(),
                        PRESIGNED_GET_URL_EXPIRATION_MINUTES
                );
            } catch (Exception e) {
                log.warn("공유 프로필 이미지 Presigned URL 생성 실패. userId={}", user.getId(), e);
            }
        }

        return PublicProfileResponseDTO.builder()
                .nickname(user.getNickName())
                .profileImageUrl(profileImageUrl)
                .introduction(user.getIntroduction())
                .representativeBooks(buildRepresentativeBooks(user.getId()))
                .build();
    }

    private List<PublicProfileResponseDTO.RepresentativeBookDto> buildRepresentativeBooks(Long userId) {
        List<UserBook> representativeBooks = userBookRepository.findRepresentativeBooks(userId);
        if (representativeBooks.isEmpty()) {
            return List.of();
        }

        List<Long> bookIds = representativeBooks.stream()
                .map(ub -> ub.getBook().getId())
                .distinct()
                .toList();
        Map<Long, Double> ratingByBookId = bookReviewRepository
                .findLatestByUserIdAndBookIds(userId, bookIds)
                .stream()
                .collect(Collectors.toMap(
                        review -> review.getMemberBook().getBook().getId(),
                        BookReview::getStar,
                        (latest, ignored) -> latest
                ));

        return representativeBooks.stream()
                .map(ub -> PublicProfileResponseDTO.RepresentativeBookDto.builder()
                        .title(ub.getBook().getTitle())
                        .author(ub.getBook().getAuthor())
                        .image(ub.getBook().getImage())
                        .displayOrder(ub.getDisplayOrder())
                        .rating(ratingByBookId.get(ub.getBook().getId()))
                        .build())
                .toList();
    }

    private String buildShareUrl(String token) {
        String baseUrl = shareWebProperties.profileWebBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.share.profile-web-base-url 설정이 필요합니다.");
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + token;
        }
        return baseUrl + "/" + token;
    }
}
