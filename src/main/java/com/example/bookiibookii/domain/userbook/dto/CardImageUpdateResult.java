package com.example.bookiibookii.domain.userbook.dto;

import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.entity.CardImage;

/**
 * Card 이미지 업데이트 결과 (서비스 내부 로직용).
 */
public record CardImageUpdateResult(Card card, CardImage cardImage, boolean isCreated) {
}
