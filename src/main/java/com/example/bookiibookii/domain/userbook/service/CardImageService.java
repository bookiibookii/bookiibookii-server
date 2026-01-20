package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.repository.CardImageRepository;
import com.example.bookiibookii.domain.userbook.repository.CardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardImageService {

    private final CardImageRepository cardImageRepository;
    private final CardRepository cardRepository;

    /**
     * CardImage 저장
     * @param cardId 카드 ID
     * @param s3Key S3 객체 키
     * @return 저장된 CardImage
     */
    @Transactional
    public CardImage saveCardImage(Long cardId, String s3Key) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + cardId));

        CardImage cardImage = CardImage.builder()
                .card(card)
                .s3Key(s3Key)
                .build();

        return cardImageRepository.save(cardImage);
    }

    /**
     * 여러 CardImage 일괄 저장
     * @param cardId 카드 ID
     * @param s3Keys S3 객체 키 리스트
     * @return 저장된 CardImage 리스트
     */
    @Transactional
    public List<CardImage> saveCardImages(Long cardId, List<String> s3Keys) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + cardId));

        List<CardImage> cardImages = s3Keys.stream()
                .map(s3Key -> CardImage.builder()
                        .card(card)
                        .s3Key(s3Key)
                        .build())
                .collect(Collectors.toList());

        return cardImageRepository.saveAll(cardImages);
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
     * Card에 속한 모든 CardImage 조회
     * @param cardId 카드 ID
     * @return CardImage 리스트
     */
    public List<CardImage> getCardImagesByCardId(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + cardId));
        return card.getCardImages();
    }
}
