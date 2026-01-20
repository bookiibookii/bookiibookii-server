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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
