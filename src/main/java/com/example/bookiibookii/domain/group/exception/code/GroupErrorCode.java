package com.example.bookiibookii.domain.group.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GroupErrorCode implements BaseCode {
    // 404 Not Found
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP404_1", "그룹을 찾을 수 없습니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP404_2", "신청 내역을 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP404_3", "해당 그룹에 신청한 내역이 없습니다."),
    PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP404_4", "해당 그룹의 파트너를 찾을 수 없습니다."),
    MATCHED_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP404_5", "해당 그룹의 멤버를 찾을 수 없습니다."),
    ADDRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP404_6", "마이페이지에서 배송지를 먼저 등록해주세요."),

    // 400 Bad Request
    ALREADY_PROCESSED_APPLICATION(HttpStatus.BAD_REQUEST, "GROUP400_1", "이미 처리된 신청 내역입니다."),
    GROUP_FULL(HttpStatus.BAD_REQUEST, "GROUP400_2", "이미 정원이 가득 찬 그룹입니다."),
    GROUP_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "GROUP400_3", "그룹이 RECRUITING 상태가 아닙니다."),
    BOOK_NOT_SELECTED(HttpStatus.BAD_REQUEST, "GROUP400_4", "도서를 선택해야 합니다."),
    INVALID_START_DATE(HttpStatus.BAD_REQUEST, "GROUP400_5", "시작 날짜는 오늘 이후여야 합니다."),
    INVALID_READING_PERIOD(HttpStatus.BAD_REQUEST, "GROUP400_6", "독서 기간은 3일에서 30일 사이여야 합니다."),
    USER_LOCATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "GROUP400_7", "직접 교환을 위해 장소를 설정해주세요."),
    INVALID_GROUP_CAPACITY(HttpStatus.BAD_REQUEST, "GROUP400_8", "함께읽기 정원은 방장 포함 2~8명 사이여야 합니다."),
    GROUP_CANT_UPDATE(HttpStatus.BAD_REQUEST, "GROUP400_9", "그룹을 수정할 수 없습니다."),
    GROUP_CANT_DELETE(HttpStatus.BAD_REQUEST, "GROUP400_10", "그룹을 삭제할 수 없습니다."),
    COMMENT_REQUIRED(HttpStatus.BAD_REQUEST, "GROUP400_11", "그룹 소개글을 입력해야 합니다."),
    APPLY_CANT_CANCEL(HttpStatus.BAD_REQUEST, "GROUP400_12", "그룹신청을 취소할 수 없습니다."),
    RECEIVER_REQUIRED(HttpStatus.BAD_REQUEST, "GROUP400_13", "단일 알림은 수신자(receiverId)가 필요합니다."),
    INVALID_GROUP_TYPE(HttpStatus.BAD_REQUEST, "GROUP400_14", "그룹 타입이 잘못되었습니다." ),
    INTRODUCTION_TOO_LONG(HttpStatus.BAD_REQUEST, "GROUP400_15", "그룹소개글은 500자 이하여야 합니다."),
    FORBIDDEN_WORD_INCLUDED(HttpStatus.BAD_REQUEST, "GROUP400_16", "금칙어가 포함되어 있습니다."),
    GUEST_MAX_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "GROUP400_17", "그룹은 3개까지 신청할 수 있습니다."),
    HOST_MAX_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "GROUP400_18", "그룹은 3개까지 생성 할 수 있습니다."),
    INVALID_GROUP_STATUS(HttpStatus.BAD_REQUEST, "GROUP400_19", "그룹상태가 진행중이 아닙니다."),
    GROUP_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "GROUP400_20", "그룹명을 입력해야 합니다."),
    INVALID_RULES(HttpStatus.BAD_REQUEST, "GROUP400_21", "규칙은 1개 이상 5개 이하로 입력해야 합니다."),

    // 403 Forbidden
    MEMBER_NOT_HOST(HttpStatus.FORBIDDEN, "GROUP403_1", "Host만 접근 가능한 메뉴입니다."),
    HOST_CANNOT_APPLY(HttpStatus.FORBIDDEN, "GROUP403_2", "Host는 신청할 수 없습니다."),
    HOST_CANNOT_LEAVE(HttpStatus.FORBIDDEN, "GROUP403_3","Host는 그룹을 떠날 수 없습니다."),
    FORBIDDEN_GROUP_ACCESS(HttpStatus.FORBIDDEN, "GROUP403_4", "해당 그룹의 멤버가 아닙니다.")

;

    private final HttpStatus status;
    private final String code;
    private final String message;

}

