package com.example.bookiibookii.domain.group.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GroupErrorCode implements BaseCode {
    // 404 Not Found
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND,"GROUP4001", "해당 그룹을 찾을 수 없습니다."),
    APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP4002", "신청 내역을 찾을 수 없습니다."),
    ALREADY_PROCESSED_APPLICATION(HttpStatus.BAD_REQUEST, "GROUP4005", "이미 처리된 신청 내역입니다."),
    GROUP_FULL(HttpStatus.BAD_REQUEST, "GROUP4006", "이미 정원이 가득 찬 그룹입니다."),

    // 403 Forbidden
    MEMBER_NOT_HOST(HttpStatus.FORBIDDEN, "GROUP4003", "Host만 접근 가능한 메뉴입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}

