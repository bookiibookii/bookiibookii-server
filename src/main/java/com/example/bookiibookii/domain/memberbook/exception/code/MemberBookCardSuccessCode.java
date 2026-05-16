package com.example.bookiibookii.domain.memberbook.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberBookCardSuccessCode implements BaseCode {

    PRESIGNED_URL_ISSUED(HttpStatus.OK, "MBCARD200_1", "카드 이미지 업로드용 Presigned URL을 발급했습니다."),
    CARD_CREATED(HttpStatus.CREATED, "MBCARD201_1", "독서카드를 생성했습니다."),
    CARD_UPDATED(HttpStatus.OK, "MBCARD200_2", "독서카드를 수정했습니다."),
    CARDS_FOUND(HttpStatus.OK, "MBCARD200_3", "독서카드 목록을 조회했습니다."),
    CARD_FOUND(HttpStatus.OK, "MBCARD200_4", "독서카드를 조회했습니다."),
    CARD_REMOVED_FROM_VIEW(HttpStatus.OK, "MBCARD200_5", "독서카드를 내 화면에서 제거했습니다."),
    BOOKMARK_TOGGLED(HttpStatus.OK, "MBCARD200_6", "북마크를 변경했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
