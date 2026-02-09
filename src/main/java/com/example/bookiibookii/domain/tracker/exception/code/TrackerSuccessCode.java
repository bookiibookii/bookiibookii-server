package com.example.bookiibookii.domain.tracker.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TrackerSuccessCode implements BaseCode {

    // --- 200 OK: 조회 관련 ---
    TRACKER_LIST_GET_OK(HttpStatus.OK, "TRACKER200_1", "나의 트래커 목록 조회가 완료되었습니다."),
    TRACKER_HOST_LIST_GET_OK(HttpStatus.OK, "TRACKER200_2", "내가 호스트인 트래커 목록 조회가 완료되었습니다."),
    TRACKER_GUEST_LIST_GET_OK(HttpStatus.OK, "TRACKER200_3", "내가 게스트인 목록 조회가 완료되었습니다."),
    TRACKER_DETAIL_GET_OK(HttpStatus.OK, "TRACKER200_4", "트래커 상세 현황 조회가 완료되었습니다."),
    TRACKER_HISTORY_GET_OK(HttpStatus.OK, "TRACKER200_5", "트래킹 히스토리 조회가 완료되었습니다."),
    TRACKER_MEETING_GET_OK(HttpStatus.OK, "TRACKER200_6", "직접 교환 약속 상세 조회가 완료되었습니다."),


    // --- 200 OK: 상태 변경 및 등록 관련 ---
    TRACKER_SHIPPING_OK(HttpStatus.OK, "TRACKER200_7", "배송 정보 등록 및 다음 주자 전달이 완료되었습니다."),
    TRACKER_RECEIVE_OK(HttpStatus.OK, "TRACKER200_8", "도서 수령 확인 및 읽기 대기 상태로 전환되었습니다."),
    TRACKER_READING_OK(HttpStatus.OK, "TRACKER200_9", "독서 시작이 등록되었습니다. 반납 예정일을 확인해주세요."),
    TRACKER_EXTENSION_OK(HttpStatus.OK, "TRACKER200_10", "독서 기간 연장이 완료되었습니다."),
    TRACKER_DONE_OK(HttpStatus.OK, "TRACKER200_11", "독서 완료가 등록되었습니다. 다음 주자에게 배송을 준비해주세요."),
    TRACKER_MEETING_UPDATE_OK(HttpStatus.OK, "TRACKER200_12", "직접 교환 약속 정보가 성공적으로 수정되었습니다."),
    TRACKER_MEETING_DONE_OK(HttpStatus.OK, "TRACKER200_13", "직접 교환이 완료되었습니다."),
    RECEPTION_VERIFIED(HttpStatus.OK, "TRACKER200_15", "상대방의 수령 인증 사진을 확인했습니다."), // 🟢 추가 및 순서 조정

    // --- 200 OK: 시스템 관련 ---
    TRACKER_CREATE_OK(HttpStatus.OK, "TRACKER200_14", "새로운 트래커가 성공적으로 생성되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}