package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.repository.CardImageRepository;
import com.example.bookiibookii.domain.userbook.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardImageService {

    private final CardImageRepository cardImageRepository;
    private final CardRepository cardRepository;

    /**
     * CardImage 저장 또는 업데이트 (카드당 이미지는 하나만 존재)
     * @param cardId 카드 ID
     * @param s3Key S3 객체 키
     * @return 저장/업데이트된 CardImage
     */
    @Transactional
    public CardImage saveOrUpdateCardImage(Long cardId, String s3Key) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + cardId));

        // 기존 이미지가 있으면 업데이트, 없으면 새로 생성
        Optional<CardImage> existingImageOpt = cardImageRepository.findByCard_Id(cardId);
        
        if (existingImageOpt.isPresent()) {
            // 기존 이미지가 있으면 삭제하고 새로 생성 (더 안전한 방식)
            CardImage existingImage = existingImageOpt.get();
            cardImageRepository.delete(existingImage);
        }
        
        // 새 이미지 생성
        CardImage cardImage = CardImage.builder()
                .card(card)
                .s3Key(s3Key)
                .build();
        return cardImageRepository.save(cardImage);
    }

    /**
     * CardImage 조회
     * @param cardImageId CardImage ID
     * @return CardImage
     */
    public CardImage getCardImage(Long cardImageId) {
        return cardImageRepository.findById(cardImageId)
                .orElseThrow(() -> new IllegalArgumentException("CardImage not found with id: " + cardImageId));
    }

    /**
     * Card에 속한 CardImage 조회 (카드당 이미지는 하나)
     * @param cardId 카드 ID
     * @return CardImage (Optional)
     */
    public Optional<CardImage> getCardImageByCardId(Long cardId) {
        return cardImageRepository.findByCard_Id(cardId);
    }

    /**
     * S3Key 중복 체크
     * @param s3Key S3 객체 키
     * @return 존재 여부
     */
    public boolean existsByS3Key(String s3Key) {
        return cardImageRepository.existsByS3Key(s3Key);
    }

    /**
     * 카드에 이미지가 존재하는지 확인
     * @param cardId 카드 ID
     * @return 존재 여부
     */
    public boolean existsByCardId(Long cardId) {
        return cardImageRepository.existsByCard_Id(cardId);
    }
}
