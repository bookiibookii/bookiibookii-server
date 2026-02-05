package com.example.bookiibookii.domain.group.controller;


import com.example.bookiibookii.domain.group.dto.req.GroupRequestDTO;
import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Group", description = "그룹 생성 및 관리 관련 API")
public interface GroupControllerDocs {
    @Operation(summary = "그룹 생성 API", description = "새로운 독서 그룹(이어읽기/함께읽기)을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_4", description = "도서 미선택"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_5", description = "부적절한 시작 날짜"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_7", description = "호스트 장소 정보 없음")
    })
    ApiResponse<GroupResponseDTO.CreateResultDTO> createGroup(
            @AuthenticationPrincipal User host,
            @RequestBody @Valid GroupRequestDTO.CreateDTO request
    );

    @Operation(summary = "그룹 정보 수정 API", description = "방장이 모집 중인 그룹의 정보를 수정합니다. (시작 날짜, 독서 기간, 소개글)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP403_1", description = "방장이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_3", description = "모집 중이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_5", description = "부적절한 시작 날짜"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_6", description = "부적절한 독서 기간"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_9", description = "RECRUITING 일떄만 수정가능")
    })
    ApiResponse<GroupResponseDTO.UpdateResultDTO> updateGroup(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal User host,
            @RequestBody @Valid GroupRequestDTO.UpdateDTO request
    );

    @Operation(summary = "그룹 삭제 API", description = "방장이 모집 중인 그룹을 삭제(Soft Delete)합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP403_1", description = "방장이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_3", description = "모집 중이 아님 (삭제 불가)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_10", description = "RECRUITING 일떄만 삭제 가능")
    })
    ApiResponse<GroupResponseDTO.DeleteResultDTO> deleteGroup(
            @PathVariable(name = "groupId")Long groupId,
            @AuthenticationPrincipal User host
    );

    @Operation(summary = "그룹 상세 조회 API", description = "특정 그룹의 상세 정보(도서, 참여 멤버, 신청 상태 등)를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP404_1", description = "존재하지 않는 그룹입니다.")
    })
    ApiResponse<GroupResponseDTO.GroupDetailDTO> getGroupDetail(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal User user
    );

    @Operation(summary = "그룹 목록 조회 API (필터/검색/추천)",
            description = "홈 화면 및 검색 페이지에서 사용하는 그룹 목록 조회 API입니다. 무한 스크롤(Slice) 방식으로 작동합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공"),
    })
    ApiResponse<GroupResponseDTO.GroupSliceResponseDTO> getGroupList(
            @AuthenticationPrincipal User user,
            @ModelAttribute GroupRequestDTO.FilterDTO filter
    );

    @Operation(summary = "그룹 통합 검색 API", description = "제목, 저자, 태그를 기반으로 그룹을 통합 검색합니다. 결과 총 건수를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공")
    })
    ApiResponse<GroupResponseDTO.SearchResultDTO> searchGroups(
            @Valid @ModelAttribute GroupRequestDTO.SearchDTO request);

    @Operation(summary = "인기 검색어 조회 API", description = "최근 실시간으로 가장 많이 검색된 상위 10개 키워드를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON200",
                              "message": "성공입니다.",
                              "result": ["슬램덩크", "한강", "채식주의자", "자바", "스프링부트"]
                            }
                            """)))
    })
    ApiResponse<List<String>> getPopularKeywords();

    @Operation(summary = "신고할 그룹 조회 API (드롭다운 데이터)",
            description = "신고하기 페이지에서 유저가 속해있는 현재 진행중(MATCHED) 상태의 그룹 데이터를 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공"),
    })
            ApiResponse<List<GroupResponseDTO.GroupSummaryResponse>> getGroupSummary(
            @AuthenticationPrincipal User user
    );

    @Operation(summary = "신고할 멤버 조회 API (드롭다운 데이터)",
            description = "신고하기 페이지에서 유저가 선택한 신고 그룹에 속해있는 멤버 데이터를 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP404_4", description = "해당 그룹의 멤버가 아닙니다.")
    })
    ApiResponse<List<GroupResponseDTO.GroupMemberResponse>> getGroupMembers(
            @AuthenticationPrincipal User user, @PathVariable(name = "groupId") Long groupId
    );

}
