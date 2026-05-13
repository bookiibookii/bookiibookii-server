package com.example.bookiibookii.domain.groupbook.dto;

import com.example.bookiibookii.domain.groupbook.entity.Card;
import com.example.bookiibookii.domain.groupbook.entity.CardImage;

/**
 * Card 이미지 업데이트 결과 (서비스 내부 로직용).
 */
public record CardImageUpdateResult(Card card, CardImage cardImage, boolean isCreated) {
}
