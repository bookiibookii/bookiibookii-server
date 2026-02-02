package com.example.bookiibookii.domain.tracker.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TrackerErrorCode implements BaseCode {

    // --- 400 BAD_REQUEST ---
    INVALID_TRACKER_STATUS(HttpStatus.BAD_REQUEST, "TRACKER400_1", "상태 변경이 불가능한 단계입니다."),
    INVALID_DELIVERY_INFO(HttpStatus.BAD_REQUEST, "TRACKER400_2", "유효하지 않은 배송 정보(운송장 번호 등)입니다."),
    EXTENSION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "TRACKER400_3" , "최대 연장 횟수를 초과했습니다." ),
    INVALID_TRACKER_DAYS(HttpStatus.BAD_REQUEST, "TRACKER400_4","0 이하의 날짜를 입력했습니다." ),
    INVALID_TRADE_TYPE(HttpStatus.BAD_REQUEST, "TRACKER400_5", "직접 교환 그룹이 아닙니다." ),
    TRACKER_NOT_CREATED(HttpStatus.BAD_REQUEST, "TRACKER400_6", "트래커 생성 중 에러 발생했습니다" ),

    // --- 403 FORBIDDEN ---
    NOT_YOUR_TURN(HttpStatus.FORBIDDEN, "TRACKER403_1", "현재 독서 주자가 아닙니다."),
    NOT_GROUP_MEMBER(HttpStatus.FORBIDDEN, "TRACKER403_2", "해당 그룹의 멤버만 트래킹 정보를 조회할 수 있습니다."),
    NOT_TRACKER_OWNER(HttpStatus.FORBIDDEN, "TRACKER403_3", "해당 트래커를 조작할 권한이 없습니다. 현재 도서 소유자만 가능합니다."),
    TRACKER_ALREADY_EXISTS(HttpStatus.FORBIDDEN, "TRACKER403_4" , "이미 트래커가 존재하는 그룹입니다." ),

    // --- 404 NOT_FOUND ---
    TRACKER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKER404_1", "해당 트래커를 찾을 수 없습니다."),
    HISTORY_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKER404_2", "트래킹 히스토리 기록이 존재하지 않습니다."),
    NEXT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKER404_3" , "다음 순번 주자를 찾을 수 없습니다." ),
    FIRST_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKER404_4", "첫 번째 주자를 찾을 수 없습니다." );


    private final HttpStatus status;
    private final String code;
    private final String message;


}