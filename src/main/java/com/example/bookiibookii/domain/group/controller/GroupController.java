package com.example.bookiibookii.domain.group.controller;

import com.example.bookiibookii.domain.group.dto.req.GroupRequestDTO;
import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.group.service.GroupService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController implements GroupControllerDocs {

    private final GroupService groupService;

    @Operation(summary = "그룹 생성 API", description = "새로운 독서 그룹(이어읽기/함께읽기)을 생성합니다.")
    @PostMapping
    public ApiResponse<GroupResponseDTO.CreateResultDTO> createGroup(
            @AuthenticationPrincipal(expression = "user") User user, // 로그인한 유저 정보
            @RequestBody @Valid GroupRequestDTO.CreateDTO request) {

        GroupResponseDTO.CreateResultDTO result = groupService.createGroup(user, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
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
            @AuthenticationPrincipal (expression = "user") User host) {

        // 실제 데이터를 지우지 않고 groupStatus를 DELETED로 변경합니다.
        GroupResponseDTO.DeleteResultDTO result = groupService.deleteGroup(groupId, host);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    //그룹 조회 API
    @GetMapping("/{groupId}")
    public ApiResponse<GroupResponseDTO.GroupDetailDTO> getGroupDetail(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal(expression = "user") User user) {
        GroupResponseDTO.GroupDetailDTO result = groupService.getGroupDetail(groupId, user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    //그룹 리스트 API
    @GetMapping
    public ApiResponse<GroupResponseDTO.GroupSliceResponseDTO> getGroupList(
            @AuthenticationPrincipal(expression = "user") User user, // 로그인 상태면 유저 정보
            @ModelAttribute @Valid GroupRequestDTO.FilterDTO filter) {

        // 서비스에서 QueryDSL을 사용하여 필터링 및 추천 가중치가 적용된 목록
        GroupResponseDTO.GroupSliceResponseDTO result = groupService.getGroupList(user, filter);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);

    }

    //그룹 검색 API
    @GetMapping("/search")
    public ApiResponse<GroupResponseDTO.SearchResultDTO> searchGroups(
            @Valid @ModelAttribute GroupRequestDTO.SearchDTO request) {

        // 서비스에서 Page 객체를 사용하여 totalCount가 포함된 결과를 가져옵니다.
        GroupResponseDTO.SearchResultDTO result = groupService.searchGroups(request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    //인기 검색어 API
    @GetMapping("/popular-keywords")
    public ApiResponse<List<String>> getPopularKeywords() {
        List<String> result = groupService.getPopularKeywords();
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    // 신고할 그룹 조회 API
    @GetMapping("/my")
    public ApiResponse<List<GroupResponseDTO.GroupSummaryResponse>> getGroupSummary(
            @AuthenticationPrincipal(expression = "user") User user) {
        List<GroupResponseDTO.GroupSummaryResponse> result = groupService.getGroupSummary(user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    // 신고할 그룹멤버 조회 API
    @GetMapping("/{groupId}/members")
    public ApiResponse<List<GroupResponseDTO.GroupMemberResponse>> getGroupMembers(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable(name = "groupId") Long groupId) {
        List<GroupResponseDTO.GroupMemberResponse> result = groupService.getGroupMembers(groupId, user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }


}
