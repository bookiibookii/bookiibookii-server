package com.example.bookiibookii.domain.notification.controller;

import com.example.bookiibookii.domain.notification.dto.NotificationResDTO;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Notification", description = "알림 관련 API")
public interface NotificationControllerDocs {

    @Operation(
            summary = "알림 목록 조회 API",
            description = """
                    카테고리별(SYSTEM/KEYWORD) 알림 목록을 조회합니다.
                    - 정렬: 안 읽은 알림 우선(read=false), 최신순(createdAt desc), id desc
                    - 무한스크롤 커서 기반 페이징을 지원합니다.
                    - cursor는 첫 요청(첫 페이지)에서는 공란(미입력)으로 호출하면 됩니다.
                    - 다음 페이지 조회 시, 이전 응답의 nextCursor 값을 cursor로 그대로 넣어 호출하세요.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 목록 조회 성공")
    })
    ApiResponse<NotificationResDTO.NotificationListRes> getNotifications(
            @AuthenticationPrincipal(expression = "user") User user,
            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "알림 카테고리 (SYSTEM 또는 KEYWORD)",
                    required = true,
                    example = "SYSTEM"
            )
            @RequestParam NotificationCategory category,

            @Parameter(
                    in = ParameterIn.QUERY,
                    description = """
                            커서 값(무한스크롤용).
                            - 첫 요청(첫 페이지)은 공란(미입력)으로 호출하면 됩니다.
                            - 다음 페이지는 이전 응답의 nextCursor 값을 그대로 넣어주세요.
                            """,
                    required = false
            )
            @RequestParam(required = false) String cursor,

            @Parameter(
                    in = ParameterIn.QUERY,
                    description = "조회 개수 (기본 20, 최대 50)",
                    required = false,
                    example = "20"
            )
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "알림 읽음 처리 API",
            description = """
                    알림을 읽음 처리합니다.
                    - 내 알림만 읽음 처리 가능합니다.
                    - 응답으로 type/payload를 내려주므로, 프론트는 이를 기반으로 화면 이동(라우팅) 처리가 가능합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공")
    })
    ApiResponse<NotificationResDTO.NotificationReadRes> markAsRead(
            @AuthenticationPrincipal(expression = "user") User user,

            @Parameter(
                    in = ParameterIn.PATH,
                    description = "읽음 처리할 알림 ID",
                    required = true,
                    example = "1"
            )
            @PathVariable Long notificationId
    );
}
