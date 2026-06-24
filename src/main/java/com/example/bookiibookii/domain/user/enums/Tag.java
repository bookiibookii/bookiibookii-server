package com.example.bookiibookii.domain.user.enums;

public enum Tag {
    MEMO("책에 직접 코멘트를 남겨요!"),
    POSTIT("직접 메모 대신 포스트잇이나 인덱스를 활용해요!"),
    PHOTO("직접 메모 대신 사진으로 기록해요!"),
    All_ROUNDER("어떤 방식이든 좋아요!"),
    NO_IDEA("아직 잘 모르겠어요."),  // 온보딩 전용
    CUSTOM(null);                    // 그룹 규칙 직접입력

    private final String defaultContent;

    Tag(String defaultContent) {
        this.defaultContent = defaultContent;
    }

    public String getDefaultContent() {
        return defaultContent;
    }
}