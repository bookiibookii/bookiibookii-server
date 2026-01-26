package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.userbook.entity.CardImage;
import com.example.bookiibookii.domain.userbook.exception.CardImageException;
import com.example.bookiibookii.domain.userbook.exception.code.CardImageErrorCode;
import com.example.bookiibookii.domain.userbook.repository.CardImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CardImageService {

    private final CardImageRepository cardImageRepository;

    // CardImage 조회 (ID로)
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
