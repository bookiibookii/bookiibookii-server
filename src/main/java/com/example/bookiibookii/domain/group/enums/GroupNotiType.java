package com.example.bookiibookii.domain.group.enums;

import com.example.bookiibookii.domain.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GroupNotiType {

    JOIN_REQUESTED(
            NotificationType.GROUP_JOIN_REQUEST,
            "똑똑! 새로운 참여 요청이 왔어요",
            "{nickname}님이 {bookTitle} 그룹에 함께하고 싶어 해요. 프로필을 확인해볼까요?",
            false
    ),

    MATCH_SUCCEEDED(
            NotificationType.GROUP_MATCH_SUCCESS,
            "그룹 매칭이 성공했어요!",
            "[{nickname}] {bookTitle} 그룹의 게스트로 참여합니다! 호스트가 책을 읽기 시작하면 알려드릴게요.",
            true
    ),

    MATCH_EXPIRED(
            NotificationType.GROUP_MATCH_FAILED_BY_EXPIRE,
            "그룹 매칭에 실패했어요",
            "기간 만료로 인해 {bookTitle} 그룹 매칭에 실패했어요. 새로운 그룹을 만들어보세요.",
            true
    ),

    MATCH_REJECTED(
            NotificationType.GROUP_MATCH_REJECTED,
            "그룹 매칭에 실패했어요",
            "[{nickname}] {bookTitle} 그룹의 정원이 마감되었어요. 다른 그룹을 찾아볼까요?",
            false
    ),

    MATCH_AUTO_REJECTED(
            NotificationType.GROUP_MATCH_AUTO_REJECTED,
            "그룹 매칭에 실패했어요",
            "[{nickname}] {bookTitle} 그룹의 정원이 마감되었어요. 다른 그룹을 찾아볼까요?",
            false
    ),

    GROUP_DELETED(
            NotificationType.GROUP_DELETED,
            "호스트가 그룹을 삭제했어요",
            "[{nickname}]님이 {bookTitle} 그룹을 삭제했어요. 호스트와 협의되지 않았다면 문의를 남겨주세요.",
            false
    );

    private final NotificationType notificationType;
    public final String title;
    public final String bodyTemplate;
    public final boolean groupIdPayload;
}