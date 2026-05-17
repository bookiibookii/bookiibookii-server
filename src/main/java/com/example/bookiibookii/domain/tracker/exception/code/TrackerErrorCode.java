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
    TRACKER_NOT_CREATED(HttpStatus.BAD_REQUEST, "TRACKER400_6", "트래커 생성 중 에러 발생했습니다"),
    INVALID_PARTNER_COUNT(HttpStatus.BAD_REQUEST, "TRACKER400_7", "1:1 교환 그룹이 아니므로 파트너를 특정할 수 없습니다."),
    INVALID_BOOK_CHANGE_TIME(HttpStatus.BAD_REQUEST, "TRACKER400_8","책 전환 시각은 null일 수 없습니다."),
    INVALID_CURRENT_MEMBER_BOOK(HttpStatus.BAD_REQUEST,"TRACKER400_8", "현재 책은 null로 변경할 수 없습니다."),
    INVALID_CURRENT_BOOK_OWNER(HttpStatus.BAD_REQUEST, "TRACKER400_9", "현재 책은 자신의 MemberBook 중에서만 선택할 수 있습니다."),
    INVALID_READING_STARTED_AT(HttpStatus.BAD_REQUEST, "TRACKER400_10", "책 전환 시각은 null일 수 없습니다."),

    // --- 403 FORBIDDEN ---
    NOT_YOUR_TURN(HttpStatus.FORBIDDEN, "TRACKER403_1", "현재 독서 주자가 아닙니다."),
    NOT_GROUP_MEMBER(HttpStatus.FORBIDDEN, "TRACKER403_2", "해당 그룹의 멤버만 트래킹 정보를 조회할 수 있습니다."),
    NOT_TRACKER_OWNER(HttpStatus.FORBIDDEN, "TRACKER403_3", "해당 트래커를 조작할 권한이 없습니다. 현재 도서 소유자만 가능합니다."),
    TRACKER_ALREADY_EXISTS(HttpStatus.FORBIDDEN, "TRACKER403_4" , "이미 트래커가 존재하는 그룹입니다." ),
    NOT_RELAY_GROUP(HttpStatus.FORBIDDEN, "TRACKER403_5", "릴레이(1:1) 그룹에서만 조회가 가능합니다."),
    // --- 404 NOT_FOUND ---
    TRACKER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKER404_1", "해당 트래커를 찾을 수 없습니다."),
    NEXT_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKER404_2", "다음 순번 주자를 찾을 수 없습니다."),
    FIRST_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKER404_3", "첫 번째 주자를 찾을 수 없습니다."),
    MEETING_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKER404_4", "예정된 약속이 없습니다."),
    PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKER404_5", "상대방 파트너 정보를 찾을 수 없습니다."),

    // --- 409 CONFLICT ---
    ALREADY_SHIPPED(HttpStatus.CONFLICT, "TRACKER409_1", "이미 이번 교환 단계에서 배송을 등록했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}