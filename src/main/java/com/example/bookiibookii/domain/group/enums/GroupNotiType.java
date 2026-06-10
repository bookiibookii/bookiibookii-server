package com.example.bookiibookii.domain.group.enums;

import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.enums.RedirectType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GroupNotiType {

    JOIN_REQUESTED(
            NotificationType.GROUP_JOIN_REQUEST,
            "새로운 참여 요청이 왔어요",
            "{nickname}님이 {groupTitle} 그룹에 참여를 신청했어요.",
            RedirectType.APPLICATION_MANAGEMENT
    ),

    MATCH_SUCCEEDED(
            NotificationType.GROUP_REQUEST_ACCEPTED,
            "참여 신청이 수락됐어요!",
            "{nickname}님의 {groupTitle} 그룹 참여가 확정됐어요. 지금 바로 확인해보세요.",
            RedirectType.TRACKER_DETAIL
    ),

    MATCH_EXPIRED(
            NotificationType.GROUP_MATCH_FAILED_BY_EXPIRE,
            "그룹 매칭에 실패했어요",
            "기간 만료로 인해 {bookTitle} 그룹 매칭에 실패했어요. 새로운 그룹을 만들어보세요.",
            RedirectType.EXPLORE_HOME
    ),

    MATCH_REJECTED(
            NotificationType.GROUP_REQUEST_REJECTED,
            "참여 신청이 거절됐어요",
            "{nickname}님의 {groupTitle} 그룹 참여 신청이 거절됐어요. 다른 그룹을 찾아볼까요?",
            RedirectType.EXPLORE_HOME
    ),

    MATCH_AUTO_REJECTED(
            NotificationType.GROUP_MATCH_AUTO_REJECTED,
            "그룹 매칭에 실패했어요",
            "[{nickname}] {bookTitle} 그룹의 정원이 마감되었어요. 다른 그룹을 찾아볼까요?",
            RedirectType.EXPLORE_HOME
    ),

    GROUP_DELETED(
            NotificationType.GROUP_DELETED,
            "호스트가 그룹을 삭제했어요",
            "[{nickname}]님이 {bookTitle} 그룹을 삭제했어요. 호스트와 협의되지 않았다면 문의를 남겨주세요.",
            RedirectType.EXPLORE_HOME
    );

    private final NotificationType notificationType;
    public final String title;
    public final String bodyTemplate;
    private final RedirectType redirectType;
}
