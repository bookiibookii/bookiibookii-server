package com.example.bookiibookii.domain.tracker.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TrackerErrorCode implements BaseCode {
    TRACKER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKER401", "해당 트래커를 찾을 수 없습니다."),
    INVALID_TRACKER_STATUS(HttpStatus.BAD_REQUEST, "TRACKER402", "상태 변경이 불가능한 단계입니다."),
    NOT_YOUR_TURN(HttpStatus.FORBIDDEN, "TRACKER403", "현재 독서 주자가 아닙니다."),

    // 1. 히스토리 관련
    HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKER404", "트래킹 히스토리 기록이 존재하지 않습니다."),

    // 2. 권한 관련 (상세 조회 시 해당 그룹 멤버가 아닐 경우)
    NOT_GROUP_MEMBER(HttpStatus.FORBIDDEN, "TRACKER405", "해당 그룹의 멤버만 트래킹 정보를 조회할 수 있습니다."),

    // 3. 배송 정보 관련 (나중에 배송지 입력 API 등을 만드실 때 유용합니다)
    INVALID_DELIVERY_INFO(HttpStatus.BAD_REQUEST, "TRACKER406", "유효하지 않은 배송 정보(운송장 번호 등)입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;


}