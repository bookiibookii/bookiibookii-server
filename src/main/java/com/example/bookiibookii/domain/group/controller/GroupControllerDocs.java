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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Group", description = "그룹 관련 API")
public interface GroupControllerDocs {
    @Operation(
            summary = "그룹 생성 API",
            description = """
                    새로운 독서 그룹을 생성합니다.
                    장소 선택 필드는 tradeType에 따라 하나만 전달합니다.
                    - DIRECT: userExchangeId에 /api/mypage/addresses/exchanges 응답의 userExchangeId(id)를 전달합니다.
                    - DELIVERY: userDeliveryId에 /api/mypage/addresses/deliveries 응답의 userDeliveryId(id)를 전달합니다.
                    locationId는 받지 않으며, 그룹 생성 시 선택 장소는 group_place 스냅샷으로 복사 저장됩니다.
                    
                    DIRECT 예시:
                    {
                      "tradeType": "DIRECT",
                      "userExchangeId": 1
                    }
                    
                    DELIVERY 예시:
                    {
                      "tradeType": "DELIVERY",
                      "userDeliveryId": 1
                    }
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_4", description = "도서 미선택"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_25", description = "DIRECT 요청에 userExchangeId 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_26", description = "DIRECT 요청에 userDeliveryId 전달"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_27", description = "DELIVERY 요청에 userDeliveryId 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_28", description = "DELIVERY 요청에 userExchangeId 전달"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_24", description = "교환 방식과 선택 장소 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP403_5", description = "본인 배송지가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP403_6", description = "본인 희망교환장소가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP404_8", description = "직접교환 장소 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP404_9", description = "배송지 없음")
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

    @Operation(
            summary = "내가 만든 그룹 목록 조회 API",
            description = """
                    현재 로그인한 사용자가 host인 그룹을 페이징 없이 최신 생성순으로 조회합니다.
                    - RECRUITING: BEFORE_MATCHING
                    - MATCHED: IN_PROGRESS
                    - COMPLETED: COMPLETED
                    - DELETED 그룹은 제외됩니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공")
    })
    ApiResponse<List<GroupResponseDTO.MyHostedGroupDTO>> getMyHostedGroups(
            @AuthenticationPrincipal User user
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
            @ParameterObject @Valid @ModelAttribute GroupRequestDTO.FilterDTO filter
    );

    @Operation(summary = "그룹 통합 검색 API", description = "제목, 저자, 태그를 기반으로 그룹을 통합 검색합니다. 결과 총 건수를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공")
    })
    ApiResponse<GroupResponseDTO.SearchResultDTO> searchGroups(
            @Valid @ModelAttribute GroupRequestDTO.SearchDTO request);

    @Operation(summary = "인기 검색어 조회 API", description = "최근 실시간으로 가장 많이 검색된 상위 10개 키워드를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공(리스트의 0번째 요소가 검색량 1위입니다.)",
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

    @Operation(summary = "그룹 홈 화면 조회 API",
            description = """
                    추천 탭의 홈 섹션을 고정 우선순위로 조회합니다.
                    각 섹션은 sectionType, title, subtitle, layoutType, items를 포함합니다.
                    데이터가 없는 그룹 카드 섹션은 sections 목록에서 제외됩니다.
                    인기 도서와 베스트셀러 책 섹션은 데이터가 없어도 빈 items로 유지됩니다.
                    홈 요청 중 외부 도서 API는 호출하지 않습니다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공")
    })
    ApiResponse<GroupResponseDTO.HomeResponseDTO> getHome(
            @AuthenticationPrincipal User user
    );

}
