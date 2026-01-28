package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import com.example.bookiibookii.domain.userbook.exception.CardImageException;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.userbook.repository.CardImageRepository;
import com.example.bookiibookii.domain.userbook.repository.CardRepository;
import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // Card의 이미지 업데이트 결과
    public record CardImageUpdateResult(Card card, CardImage cardImage, boolean isCreated) {}

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

        // Card 생성
        Card card = Card.builder()
                .userBook(userBook)
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
            if (cardImageRepository.existsByS3Key(s3Key)) {
                throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
            }
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }

        return savedCard;
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
}
