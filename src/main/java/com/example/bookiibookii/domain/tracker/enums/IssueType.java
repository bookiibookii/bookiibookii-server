package com.example.bookiibookii.domain.tracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum IssueType {

    // 1. 배송 관련 이슈
    NO_SHIPPING("도서 미발송", "정해진 기간 내에 책을 보내지 않았습니다."),
    FAKE_TRACKING("허위 송장", "유효하지 않은 송장 번호를 입력했습니다."),
    SHIPPING_LOSS("배송 분실", "택배가 도착하지 않았거나 분실되었습니다."),

    // 2. 도서 상태 관련 이슈
    DAMAGED_BOOK("도서 파손/훼손", "수령한 책이 설명과 다르게 파손되어 있습니다."),
    WRONG_BOOK("다른 도서 배송", "신청한 책이 아닌 다른 책이 배송되었습니다."),

    // 3. 기타/사용자 입력 이슈
    ETC("기타", "직접 입력한 사유로 신고합니다.");

    private final String title;
    private final String description;

}
