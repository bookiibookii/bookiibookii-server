package com.example.bookiibookii.domain.userbook.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CardImageSuccessCode implements BaseCode {
    PRESIGNED_URL_ISSUED(HttpStatus.OK, "CARDIMG200_1", "카드 이미지 업로드용 Presigned URL을 발급했습니다."),
    CARD_IMAGE_SAVED(HttpStatus.CREATED, "CARDIMG201_1", "카드 이미지를 저장했습니다."),
    CARD_IMAGE_UPDATED(HttpStatus.OK, "CARDIMG200_3", "카드 이미지를 업데이트했습니다."),
    CARD_IMAGE_FOUND(HttpStatus.OK, "CARDIMG200_2", "카드 이미지를 조회했습니다."),
    CARD_FOUND(HttpStatus.OK, "CARDIMG200_6", "독서카드를 조회했습니다."),
    CARD_CREATED(HttpStatus.CREATED, "CARDIMG201_2", "독서카드를 생성했습니다."),
    CARDS_FOUND(HttpStatus.OK, "CARDIMG200_4", "독서카드 목록을 조회했습니다."),
    CARD_UPDATED(HttpStatus.OK, "CARDIMG200_5", "독서카드를 수정했습니다."),
    CARD_REMOVED_FROM_VIEW(HttpStatus.OK, "CARDIMG200_9", "독서카드를 내 화면에서 제거했습니다."),
    BOOKMARK_TOGGLED(HttpStatus.OK, "CARDIMG200_7", "북마크를 변경했습니다."),
    BOOKMARKED_CARDS_FOUND(HttpStatus.OK, "CARDIMG200_8", "북마크한 독서카드 목록을 조회했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
