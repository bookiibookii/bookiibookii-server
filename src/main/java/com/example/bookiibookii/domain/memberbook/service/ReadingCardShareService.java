package com.example.bookiibookii.domain.memberbook.service;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.dto.res.PublicReadingCardResponseDTO;
import com.example.bookiibookii.domain.memberbook.dto.res.ShareTokenResponseDTO;
import com.example.bookiibookii.domain.memberbook.entity.CardShareToken;
import com.example.bookiibookii.domain.memberbook.entity.Cards;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.entity.MemberCard;
import com.example.bookiibookii.domain.memberbook.enums.CardType;
import com.example.bookiibookii.domain.memberbook.exception.MemberBookException;
import com.example.bookiibookii.domain.memberbook.exception.code.MemberBookErrorCode;
import com.example.bookiibookii.domain.memberbook.repository.CardShareTokenRepository;
import com.example.bookiibookii.domain.memberbook.repository.CardsRepository;
import com.example.bookiibookii.domain.memberbook.repository.MemberCardRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.global.config.ShareWebProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReadingCardShareService {

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    private final CardsRepository cardsRepository;
    private final CardShareTokenRepository cardShareTokenRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final MemberCardRepository memberCardRepository;
    private final CardImageS3Service cardImageS3Service;
    private final ShareWebProperties shareWebProperties;
    private final UserRepository userRepository;

    @Transactional
    public ShareTokenResponseDTO createShareToken(Long cardId, User user) {
        Cards card = getShareableCardForMember(cardId, user.getId());
        revokeActiveTokensForCard(card.getId());

        CardShareToken shareToken = cardShareTokenRepository.save(
                CardShareToken.create(card, userRepository.getReferenceById(user.getId()))
        );

        return ShareTokenResponseDTO.builder()
                .shareToken(shareToken.getToken())
                .shareUrl(buildShareUrl(shareToken.getToken()))
                .build();
    }

    @Transactional(readOnly = true)
    public PublicReadingCardResponseDTO getPublicReadingCard(String token) {
        CardShareToken shareToken = cardShareTokenRepository.findActiveByTokenWithCardDetails(token)
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.SHARE_TOKEN_NOT_FOUND));

        Cards card = shareToken.getCard();
        validateShareableForPublic(card);

        return toPublicResponse(card);
    }

    @Transactional
    public void revokeActiveTokensForCard(Long cardId) {
        LocalDateTime now = LocalDateTime.now();
        List<CardShareToken> activeTokens = cardShareTokenRepository.findAllActiveByCardId(cardId);
        activeTokens.forEach(token -> token.revoke(now));
    }

    private Cards getShareableCardForMember(Long cardId, Long userId) {
        Cards card = cardsRepository.findByIdWithDetails(cardId)
                .orElseThrow(() -> new MemberBookException(MemberBookErrorCode.MEMBER_CARD_NOT_FOUND));

        MemberBook memberBook = card.getMemberBook();
        if (memberBook == null || memberBook.getMatchedMember() == null || memberBook.getGroup() == null) {
            throw new MemberBookException(MemberBookErrorCode.MEMBER_CARD_NOT_FOUND);
        }

        Long groupId = memberBook.getGroup().getId();
        if (!matchedMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId)) {
            throw new MemberBookException(MemberBookErrorCode.MEMBER_CARD_NOT_FOUND);
        }

        validateShareable(card);
        return card;
    }

    private void validateShareableForPublic(Cards card) {
        try {
            validateShareable(card);
        } catch (MemberBookException e) {
            if (e.getCode() == MemberBookErrorCode.CARD_NOT_SHAREABLE) {
                throw new MemberBookException(MemberBookErrorCode.SHARE_TOKEN_NOT_FOUND);
            }
            throw e;
        }
    }

    private void validateShareable(Cards card) {
        if (card.getDeletedAt() != null) {
            throw new MemberBookException(MemberBookErrorCode.CARD_NOT_SHAREABLE);
        }

        MemberBook memberBook = card.getMemberBook();
        if (memberBook.getRemovedAt() != null) {
            throw new MemberBookException(MemberBookErrorCode.CARD_NOT_SHAREABLE);
        }

        MatchedMember owner = memberBook.getMatchedMember();
        memberCardRepository.findByMatchedMember_IdAndCard_Id(owner.getId(), card.getId())
                .filter(MemberCard::isHidden)
                .ifPresent(state -> {
                    throw new MemberBookException(MemberBookErrorCode.CARD_NOT_SHAREABLE);
                });
    }

    private PublicReadingCardResponseDTO toPublicResponse(Cards card) {
        MemberBook memberBook = card.getMemberBook();
        var book = memberBook.getBook();
        var owner = memberBook.getMatchedMember().getUser();

        String imageUrl = null;
        if (card.getCardType() == CardType.IMAGE && card.getCardImages() != null) {
            try {
                imageUrl = cardImageS3Service.generatePresignedGetUrl(
                        card.getCardImages().getS3Key(),
                        PRESIGNED_GET_URL_EXPIRATION_MINUTES
                );
            } catch (Exception e) {
                log.warn("공유 카드 이미지 Presigned URL 생성 실패. cardId={}", card.getId(), e);
            }
        }

        return PublicReadingCardResponseDTO.builder()
                .cardType(card.getCardType())
                .bookTitle(book.getTitle())
                .bookAuthor(book.getAuthor())
                .bookImage(book.getImage())
                .creatorNickname(owner.getNickName())
                .page(card.getPage())
                .memo(card.getMemo())
                .quotation(card.getQuotation())
                .imageUrl(imageUrl)
                .build();
    }

    private String buildShareUrl(String token) {
        String baseUrl = shareWebProperties.webBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.share.web-base-url 설정이 필요합니다.");
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl + token;
        }
        return baseUrl + "/" + token;
    }
}
