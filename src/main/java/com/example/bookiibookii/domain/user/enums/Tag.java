package com.example.bookiibookii.domain.user.enums;

public enum Tag {
    POSTIT, // 온보딩  태그 : 포스트잇이나 인덱스를 활용
    PHOTO,  // 온보딩 태그 : 사진을 찍어 기록해요
    NO_RECORD,  // 온보딩 태그 : 기록보다 읽는 것에 집중해요
    NO_IDEA,   // 온보딩 태그 : 아직 잘 모르겠어요
    MEMO,   // 온보딩 + 그룹생성 태그 : 펜으로 밑줄을 긋고 메모
    CLEAN,  // 그룹생성 태그 : 책을 깨끗하게 읽어주세요
    FREE_STYLE, // 그룹생성 태그 : 책이 훼손되지 않는 선에서 자유롭게 읽어요
    CUSTOM
}