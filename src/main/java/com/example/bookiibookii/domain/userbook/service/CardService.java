package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.userbook.dto.res.CardCreateResponseDTO;
import com.example.bookiibookii.domain.userbook.dto.res.CardImageResponseDTO;
import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import com.example.bookiibookii.domain.userbook.exception.CardImageException;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.userbook.repository.CardImageRepository;
import com.example.bookiibookii.domain.userbook.repository.CardRepository;
import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardService {

    private final CardRepository cardRepository;
    private final CardImageRepository cardImageRepository;
    private final CardImageValidationService cardImageValidationService;
    private final CardImageS3Service cardImageS3Service;
    private final UserBookRepository userBookRepository;
    private final MatchedMemberRepository matchedMemberRepository;

    // Card의 이미지 업데이트 결과
    public record CardImageUpdateResult(Card card, CardImage cardImage, boolean isCreated) {}

    // UserBook 책 제목 + 그룹 ID + 카드 목록 조회 결과
    public record CardsWithTitleResult(String title, Long groupId, List<CardCreateResponseDTO> cards) {}

    /**
     * 독서카드를 생성합니다.
     * Card는 항상 CardImage를 가져야 하므로, Card와 CardImage를 함께 생성합니다.
     * 
     * @param userBookId 사용자 책 ID
     * @param userId 인증된 사용자 ID (소유권 검증용)
     * @param page 페이지 정보
     * @param memo 메모 (선택)
     * @param s3Key S3 키 (이미 업로드된 이미지)
     * @return 생성된 Card
     */
    @Transactional
    public Card createCard(Long userBookId, Long userId, Integer page, String memo, String s3Key) {
        // s3Key 형식 검증
        if (!cardImageValidationService.isValidS3Key(s3Key)) {
            throw new CardImageException(CardImageErrorCode.INVALID_S3_KEY_FORMAT);
        }

        // S3에 이미지가 실제로 존재하는지 확인 (HEAD 요청)
        if (!cardImageS3Service.doesImageExist(s3Key)) {
            throw new CardImageException(CardImageErrorCode.IMAGE_NOT_FOUND_IN_S3);
        }

        // UserBook 존재 및 소유권 확인
        UserBook userBook = userBookRepository.findByIdAndUser_Id(userBookId, userId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.USER_BOOK_NOT_FOUND));

        // s3Key 중복 체크 (DB)
        if (cardImageRepository.existsByS3Key(s3Key)) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }

        // Card 생성 (그룹 멤버들이 함께 볼 수 있도록 group 매핑)
        Card card = Card.builder()
                .userBook(userBook)
                .group(userBook.getGroup())
                .page(page)
                .memo(memo)
                .build();

        Card savedCard = cardRepository.save(card);

        // CardImage 생성
        CardImage cardImage = CardImage.builder()
                .card(savedCard)
                .s3Key(s3Key)
                .build();

        try {
            cardImageRepository.saveAndFlush(cardImage);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // s3_key unique constraint 위반 처리
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }

        return savedCard;
    }

    /**
     * 독서카드를 수정합니다.
     * page, memo, s3Key 중 전달된 필드만 변경됩니다.
     * 카드 소유자(UserBook 소유자)만 수정 가능합니다.
     *
     * @param cardId 카드 ID
     * @param userId 인증된 사용자 ID (권한 검증용)
     * @param page   변경할 페이지 (null이면 미변경)
     * @param memo   변경할 메모 (null이면 미변경)
     * @param s3Key  변경할 카드 이미지 S3 키 (null이면 미변경)
     * @return 수정된 Card (cardImage fetch)
     */
    @Transactional
    public Card updateCard(Long cardId, Long userId, Integer page, String memo, String s3Key) {
        Card card = cardRepository.findByIdWithUserBookAndGroup(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));

        boolean isOwner = card.getUserBook().getUser().getId().equals(userId);
        if (!isOwner) {
            throw new CardImageException(CardImageErrorCode.CARD_NOT_FOUND);
        }

        if (page != null) {
            card.updatePage(page);
        }
        if (memo != null) {
            card.updateMemo(memo);
        }
        if (s3Key != null && !s3Key.isBlank()) {
            updateCardImage(cardId, s3Key);
        }

        cardRepository.flush();
        return cardRepository.findByIdWithCardImage(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));
    }

    /**
     * Card의 이미지를 업데이트합니다.
     * Card는 항상 CardImage를 가져야 하므로, Card와 함께 관리됩니다.
     * 
     * @param cardId 카드 ID
     * @param s3Key 새로운 S3 Key
     * @return 업데이트 결과
     */
    @Transactional
    public CardImageUpdateResult updateCardImage(Long cardId, String s3Key) {
        // s3Key 형식 검증
        if (!cardImageValidationService.isValidS3Key(s3Key)) {
            throw new CardImageException(CardImageErrorCode.INVALID_S3_KEY_FORMAT);
        }

        // S3에 이미지가 실제로 존재하는지 확인 (HEAD 요청)
        if (!cardImageS3Service.doesImageExist(s3Key)) {
            throw new CardImageException(CardImageErrorCode.IMAGE_NOT_FOUND_IN_S3);
        }

        // Card 조회
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));

        // S3Key 중복 체크 (다른 카드에서 사용 중인지 확인)
        if (cardImageRepository.existsByS3KeyAndCard_IdNot(s3Key, cardId)) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }

        // Card의 기존 CardImage 조회
        Optional<CardImage> existingImageOpt = cardImageRepository.findByCard_Id(cardId);
        
        if (existingImageOpt.isPresent()) {
            // 기존 이미지가 있으면 업데이트
            CardImage existingImage = existingImageOpt.get();
            // s3Key가 동일하면 업데이트 불필요
            if (existingImage.getS3Key().equals(s3Key)) {
                return new CardImageUpdateResult(card, existingImage, false);
            }
            // 기존 엔티티의 s3Key만 업데이트
            existingImage.updateS3Key(s3Key);
            try {
                CardImage updatedImage = cardImageRepository.saveAndFlush(existingImage);
                return new CardImageUpdateResult(card, updatedImage, false);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // s3_key unique constraint 위반 처리
                Throwable rootCause = e.getRootCause();
                String errorMessage = "";
                
                if (rootCause instanceof java.sql.SQLException) {
                    errorMessage = rootCause.getMessage() != null ? rootCause.getMessage().toLowerCase() : "";
                } else if (rootCause != null) {
                    errorMessage = rootCause.getMessage() != null ? rootCause.getMessage().toLowerCase() : "";
                } else {
                    errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                }
                
                // s3_key unique constraint 위반 확인
                if (errorMessage.contains("s3_key") || errorMessage.contains("'s3_key'")) {
                    throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
                }
                
                // 기타 제약조건 위반 (race condition 등)
                if (cardImageRepository.existsByS3Key(s3Key)) {
                    throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
                }
                
                // 기타 제약조건 위반은 s3_key 중복으로 간주
                throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
            }
        }
        
        // Card는 있지만 CardImage가 없는 경우 (이론적으로 발생하지 않아야 함)
        // optional = false이므로 항상 CardImage가 있어야 하지만, 안전을 위해 처리
        CardImage newCardImage = CardImage.builder()
                .card(card)
                .s3Key(s3Key)
                .build();
        
        try {
            CardImage savedImage = cardImageRepository.saveAndFlush(newCardImage);
            return new CardImageUpdateResult(card, savedImage, true);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // s3_key unique constraint 위반 처리
            Throwable rootCause = e.getRootCause();
            String errorMessage = "";
            
            if (rootCause instanceof java.sql.SQLException) {
                errorMessage = rootCause.getMessage() != null ? rootCause.getMessage().toLowerCase() : "";
            } else if (rootCause != null) {
                errorMessage = rootCause.getMessage() != null ? rootCause.getMessage().toLowerCase() : "";
            } else {
                errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            }
            
            // s3_key unique constraint 위반 확인
            if (errorMessage.contains("s3_key") || errorMessage.contains("'s3_key'")) {
                throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
            }
            
            // 기타 제약조건 위반
            if (cardImageRepository.existsByS3Key(s3Key)) {
                throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
            }
            
            // card_id unique constraint 위반 (race condition)
            Optional<CardImage> raceConditionImage = cardImageRepository.findByCard_Id(cardId);
            if (raceConditionImage.isPresent()) {
                CardImage existingImage = raceConditionImage.get();
                existingImage.updateS3Key(s3Key);
                try {
                    CardImage updatedImage = cardImageRepository.saveAndFlush(existingImage);
                    return new CardImageUpdateResult(card, updatedImage, false);
                } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                    throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
                }
            }
            
            // 기타 제약조건 위반은 s3_key 중복으로 간주
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }
    }

    /**
     * Card를 조회합니다.
     * Card는 항상 CardImage를 포함합니다 (optional = false).
     */
    public Card getCard(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));
    }

    /**
     * Card의 CardImage를 조회합니다.
     */
    public CardImage getCardImage(Long cardId) {
        return cardImageRepository.findByCard_Id(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_IMAGE_NOT_FOUND));
    }

    /**
     * 카드 상세 조회용으로 Card를 CardImage와 함께 조회합니다.
     */
    public Card getCardWithCardImage(Long cardId) {
        return cardRepository.findByIdWithCardImage(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));
    }

    /**
     * 카드 상세 조회용으로 Card를 CardImage, UserBook, Group, Book과 함께 조회합니다.
     * 책 제목 등 상세 응답에 필요한 연관 데이터를 한 번에 로드합니다.
     */
    public Card getCardWithCardImageAndBook(Long cardId) {
        return cardRepository.findByIdWithCardImageAndUserBookAndBook(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));
    }

    /**
     * 카드 상세 조회. 카드 소유자(UserBook 소유자)이거나 같은 그룹 멤버만 조회 가능합니다.
     */
    public Card getCardDetail(Long cardId, Long userId) {
        Card card = cardRepository.findByIdWithCardImageAndUserBookAndBook(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));

        Long groupId = card.getGroup().getGroupId();
        boolean isOwner = card.getUserBook().getUser().getId().equals(userId);
        boolean isGroupMember = matchedMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, userId);
        if (!isOwner && !isGroupMember) {
            throw new CardImageException(CardImageErrorCode.CARD_NOT_FOUND);
        }

        return card;
    }

    /**
     * UserBook에 속한 Card 목록과 해당 UserBook의 책 제목을 조회합니다.
     * UserBook 소유자이거나 같은 그룹 멤버인 경우에만 조회 가능합니다.
     * 책 제목, groupId, 카드 목록을 생성일 기준 오름차순으로 반환합니다.
     */
    public CardsWithTitleResult getCardsByUserBookId(Long userBookId, Long userId, int presignedGetUrlExpirationMinutes) {
        UserBook userBook = userBookRepository.findByIdWithGroupAndUser(userBookId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.USER_BOOK_NOT_FOUND));

        Long groupId = userBook.getGroup().getGroupId();
        boolean isOwner = userBook.getUser().getId().equals(userId);
        boolean isGroupMember = matchedMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, userId);
        if (!isOwner && !isGroupMember) {
            throw new CardImageException(CardImageErrorCode.USER_BOOK_NOT_FOUND);
        }

        // Book 제목 조회 (Group → Book)
        String title = userBook.getGroup().getBook().getTitle();
        
        List<Card> cards = cardRepository.findByUserBookIdWithCardImage(userBookId);
        
        // Card 엔티티를 DTO로 변환 (트랜잭션 내에서 처리)
        List<CardCreateResponseDTO> cardDTOs = cards.stream()
                .map(card -> {
                    CardImage cardImage = card.getCardImage();
                    if (cardImage == null) {
                        throw new CardImageException(CardImageErrorCode.CARD_IMAGE_NOT_FOUND);
                    }
                    
                    CardImageResponseDTO cardImageResponseDTO = CardImageResponseDTO.builder()
                            .cardImageId(cardImage.getId())
                            .s3Key(cardImage.getS3Key())
                            .presignedGetUrl(cardImageS3Service.generatePresignedGetUrl(
                                    cardImage.getS3Key(),
                                    presignedGetUrlExpirationMinutes))
                            .build();
                    
                    return CardCreateResponseDTO.builder()
                            .cardId(card.getId())
                            .page(card.getPage())
                            .memo(card.getMemo())
                            .cardImage(cardImageResponseDTO)
                            .createdAt(card.getCreatedAt())
                            .build();
                })
                .toList();
        
        return new CardsWithTitleResult(title, groupId, cardDTOs);
    }
}
