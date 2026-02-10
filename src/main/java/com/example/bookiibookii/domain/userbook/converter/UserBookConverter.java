package com.example.bookiibookii.domain.userbook.converter;

import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.domain.userbook.dto.res.*;
import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import com.example.bookiibookii.domain.userbook.exception.CardImageException;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.userbook.service.CardImageS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserBookConverter {
    private final CardImageS3Service cardImageS3Service;
    private final UserImageS3Service userImageS3Service;

    /**
     * CardImage -> CardImageResponseDTO 변환
     */
    public CardImageResponseDTO toCardImageResponseDTO(CardImage cardImage, int expirationMinutes) {
        if (cardImage == null) return null;
        return CardImageResponseDTO.builder()
                .cardImageId(cardImage.getId())
                .s3Key(cardImage.getS3Key())
                .presignedGetUrl(cardImageS3Service.generatePresignedGetUrl(cardImage.getS3Key(), expirationMinutes))
                .build();
    }

    /**
     * 카드 생성/수정 성공 응답 DTO 변환
     */
    public CardCreateResponseDTO toCardCreateResponseDTO(Card card, CardImage cardImage, int expirationMinutes) {
        return CardCreateResponseDTO.builder()
                .cardId(card.getId())
                .page(card.getPage())
                .memo(card.getMemo())
                .cardImage(toCardImageResponseDTO(cardImage, expirationMinutes))
                .createdAt(card.getCreatedAt())
                .build();
    }

    /**
     * 그룹 카드 목록/상세 조회를 위한 DTO 변환
     */
    public GroupCardResponseDTO toGroupCardResponseDTO(Card card, CardImage cardImage,
                                                       int expirationMinutes, String bookTitle,
                                                       boolean isBookmarked, String creatorName) {
        return GroupCardResponseDTO.builder()
                .cardId(card.getId())
                .page(card.getPage())
                .memo(card.getMemo())
                .cardImage(toCardImageResponseDTO(cardImage, expirationMinutes))
                .createdAt(card.getCreatedAt())
                .bookTitle(bookTitle)
                .isBookmarked(isBookmarked)
                .creatorName(creatorName)
                .build();
    }

    /**
     * 전체 카드 리스트 응답 DTO 변환
     */
    public CardListResponseDTO toCardListResponse(Long groupId, CardListResponseDTO.CurrentBookOwnerDto currentBookOwner,
                                                  String myComment, String partnerComment, List<GroupCardResponseDTO> cardDTOs) {
        return CardListResponseDTO.builder()
                .groupId(groupId)
                .currentBookOwner(currentBookOwner)
                .myComment(myComment)
                .partnerComment(partnerComment)
                .cards(cardDTOs)
                .build();
    }

    /**
     * UserBook 엔티티를 LibraryBookResponseDTO로 변환
     */
    public LibraryBookResponseDTO toLibraryBookResponseDTO(UserBook ub, Tracker tracker, int expirationMinutes) {
        if (ub.getGroup() == null || ub.getGroup().getBook() == null || ub.getGroup().getHost() == null) {
            throw new CardImageException(CardImageErrorCode.USER_BOOK_NOT_FOUND);
        }

        var group = ub.getGroup();
        var book = group.getBook();
        var host = group.getHost();

        // 호스트 프로필 이미지 URL 생성
        String hostProfileImageUrl = null;
        if (host.getUserImage() != null) {
            try {
                hostProfileImageUrl = userImageS3Service.generatePresignedGetUrl(
                        host.getUserImage().getS3Key(), expirationMinutes);
            } catch (Exception e) {
                log.warn("호스트 프로필 이미지 Presigned URL 생성 실패: {}", e.getMessage());
            }
        }

        // 종료 날짜 계산 로직
        LocalDate finalEndDate = null;
        if (tracker != null && tracker.getEndDate() != null) {
            finalEndDate = tracker.getEndDate().toLocalDate();
        } else if (group.getStartDate() != null && group.getReadingPeriod() != null) {
            finalEndDate = group.getStartDate().plusDays(group.getReadingPeriod());
        }

        return LibraryBookResponseDTO.builder()
                .groupId(group.getGroupId())
                .userBookId(ub.getId())
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthor())
                .image(book.getImage())
                .hostId(host.getId())
                .hostNickName(host.getNickName())
                .hostProfileImageUrl(hostProfileImageUrl)
                .groupType(group.getGroupType())
                .startDate(group.getStartDate())
                .endDate(finalEndDate)
                .duration(group.getReadingPeriod())
                .rating(ub.getRating())
                .comment(ub.getComment())
                .build();
    }
}
