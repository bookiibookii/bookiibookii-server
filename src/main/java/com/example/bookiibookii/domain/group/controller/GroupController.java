package com.example.bookiibookii.domain.group.controller;

import com.example.bookiibookii.domain.group.dto.req.GroupRequestDTO;
import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.group.service.GroupService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Group API", description = "그룹 생성 및 조회 관련 API")
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController implements GroupControllerDocs{

    private final GroupService groupService;

    @Operation(summary = "그룹 생성 API", description = "새로운 독서 그룹(이어읽기/함께읽기)을 생성합니다.")
    @PostMapping
    public ApiResponse<GroupResponseDTO.CreateResultDTO> createGroup(
            @AuthenticationPrincipal User host, // 로그인한 유저 정보
            @RequestBody @Valid GroupRequestDTO.CreateDTO request) {

        GroupResponseDTO.CreateResultDTO result = groupService.createGroup(host, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK,result);
    }

    @PatchMapping("/{groupId}")
    public ApiResponse<GroupResponseDTO.UpdateResultDTO> updateGroup(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal User host,
            @RequestBody @Valid GroupRequestDTO.UpdateDTO request) { // @RequestBody와 @Valid 추가

        // 서비스에서 비관적 락(Pessimistic Lock)과 RECRUITING 상태 체크를 수행합니다.
        GroupResponseDTO.UpdateResultDTO result = groupService.updateGroup(groupId, host, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    // 그룹 삭제 API (DELETE - Soft Delete 방식)
    @DeleteMapping("/{groupId}")
    public ApiResponse<GroupResponseDTO.DeleteResultDTO> deleteGroup(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal User host) {

        // 실제 데이터를 지우지 않고 groupStatus를 DELETED로 변경합니다.
        GroupResponseDTO.DeleteResultDTO result = groupService.deleteGroup(groupId, host);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    //그룹 조회 API
    @GetMapping("/{groupId}")
    public ApiResponse<GroupResponseDTO.GroupDetailDTO> getGroupDetail(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal User user) {
        GroupResponseDTO.GroupDetailDTO result = groupService.getGroupDetail(groupId, user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }
}
