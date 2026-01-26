package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.exception.CardImageException;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageErrorCode;
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
    private final CardImageValidationService cardImageValidationService;

    // CardImage 저장 또는 업데이트 결과
    public record SaveOrUpdateResult(CardImage cardImage, boolean isCreated) {}

    // CardImage 저장 또는 업데이트 (검증 포함)

    @Transactional
    public SaveOrUpdateResult saveOrUpdateCardImage(Long cardId, String s3Key) {
        // s3Key 검증: 형식 및 cardId 일치 확인
        if (!cardImageValidationService.isValidS3Key(s3Key, cardId)) {
            throw new CardImageException(CardImageErrorCode.INVALID_S3_KEY_FORMAT);
        }

        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_NOT_FOUND));

        // S3Key 중복 체크 (다른 카드에서 사용 중인지 확인)
        if (existsByS3KeyForOtherCard(cardId, s3Key)) {
            throw new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY);
        }

        // 기존 이미지가 있으면 업데이트, 없으면 새로 생성
        Optional<CardImage> existingImageOpt = cardImageRepository.findByCard_Id(cardId);
        
        boolean isCreated;
        CardImage cardImage;
        
        if (existingImageOpt.isPresent()) {
            // 기존 이미지가 있으면 업데이트
            CardImage existingImage = existingImageOpt.get();
            // s3Key가 동일하면 업데이트 불필요
            if (existingImage.getS3Key().equals(s3Key)) {
                return new SaveOrUpdateResult(existingImage, false);
            }
            cardImageRepository.delete(existingImage);
            isCreated = false;
        } else {
            isCreated = true;
        }
        
        // 새 이미지 생성
        cardImage = CardImage.builder()
                .card(card)
                .s3Key(s3Key)
                .build();
        
        try {
            CardImage savedImage = cardImageRepository.save(cardImage);
            return new SaveOrUpdateResult(savedImage, isCreated);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 동일 cardId에 대한 중복 삽입 시도 시, 기존 이미지를 다시 조회하여 반환
            CardImage existingImage = cardImageRepository.findByCard_Id(cardId)
                    .orElseThrow(() -> new CardImageException(CardImageErrorCode.DUPLICATE_S3_KEY));
            return new SaveOrUpdateResult(existingImage, false);
        }
    }

    // CardImage 조회

    public CardImage getCardImage(Long cardImageId) {
        return cardImageRepository.findById(cardImageId)
                .orElseThrow(() -> new CardImageException(CardImageErrorCode.CARD_IMAGE_NOT_FOUND));
    }

    // Card에 속한 CardImage 조회 (카드당 이미지는 하나)
    public Optional<CardImage> getCardImageByCardId(Long cardId) {
        return cardImageRepository.findByCard_Id(cardId);
    }

    // S3Key 중복 체크

    public boolean existsByS3Key(String s3Key) {
        return cardImageRepository.existsByS3Key(s3Key);
    }

    // 다른 카드에서 S3Key 사용 중인지 확인
    public boolean existsByS3KeyForOtherCard(Long cardId, String s3Key) {
        return cardImageRepository.existsByS3KeyAndCard_IdNot(s3Key, cardId);
    }

    // 카드에 이미지가 존재하는지 확인
    public boolean existsByCardId(Long cardId) {
        return cardImageRepository.existsByCard_Id(cardId);
    }
}
