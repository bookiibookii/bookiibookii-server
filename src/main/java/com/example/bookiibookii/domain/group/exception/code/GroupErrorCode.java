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

    // 400 Bad Request
    ALREADY_PROCESSED_APPLICATION(HttpStatus.BAD_REQUEST, "GROUP400_1", "이미 처리된 신청 내역입니다."),
    GROUP_FULL(HttpStatus.BAD_REQUEST, "GROUP400_2", "이미 정원이 가득 찬 그룹입니다."),
    GROUP_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "GROUP400_3", "그룹이 RECRUITING 상태가 아닙니다."),

    // 403 Forbidden
    MEMBER_NOT_HOST(HttpStatus.FORBIDDEN, "GROUP403_1", "Host만 접근 가능한 메뉴입니다."),
    HOST_CANNOT_APPLY(HttpStatus.FORBIDDEN, "GROUP403_2", "Host는 신청할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}

