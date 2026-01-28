package com.example.bookiibookii.domain.tracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrackerImageType {
    SENDER_PROOF("보낸 사람 인증 이미지"),
    RECEIVER_PROOF("받은 사람 인증 이미지");

    private final String description;
}
