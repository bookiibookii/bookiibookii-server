package com.example.bookiibookii.domain.user.enums;

import lombok.Getter;

@Getter
public enum Badge {
    KINDNESS("친절하고 매너가 좋아요"),
    GOOD_HANDWRITING("글씨가 예뻐요"),
    SWEET_COMMENT("코멘트가 다정해요"),
    INSIGHTFUL("책에 대한 인사이트가 넘쳐요"),
    FAST_SHIPPING("책을 빠르게 보내줬어요"),
    FUNNY("코멘트가 재미있어요"),
    CLEAN_CONDITION("책을 깨끗하고 깔끔하게 읽어요");

    private final String description;

    Badge(String description) { this.description = description; }
}
