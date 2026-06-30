package com.example.bookiibookii.domain.group.controller;

import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Admin", description = "관리자용 API")
public interface AdminGroupControllerDocs {

    @Operation(
            summary = "그룹 강제 종료 API",
            description = """
            관리자가 그룹을 강제 종료합니다.
            - RECRUITING: PENDING 신청 거절 후 그룹 삭제. 호스트 및 신청자에게 알림 발송.
            - MATCHED: MatchedMember 완료 처리 후 그룹 삭제. 멤버에게 알림 발송.
            - COMPLETED / DELETED: 400 에러 반환.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "그룹 강제 종료 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이미 종료된 그룹"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    ApiResponse<Void> forceCloseGroup(@PathVariable Long groupId);
}
