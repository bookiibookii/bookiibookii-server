package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.dto.req.MeetingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.MeetingDefaultPlaceResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.MeetingResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "DirectExchange", description = "직접 교환 관련 API")
@RequestMapping("/api/groups/{groupId}/meetings")
public interface MeetingControllerDocs {

    @PostMapping
    @Operation(
            summary = "직접 교환 약속 등록",
            description = """
            HOST가 직접교환 약속을 등록합니다.
            
            - 직접교환 그룹에서만 가능합니다.
            - 두 matchedMember 모두 EXCHANGING 또는 모두 RETURNING 상태여야 합니다.
            - 등록 성공 시 두 matchedMember의 ExchangeStatus가 MEETING_SCHEDULED로 변경됩니다.
            - meetingAt이 미래여도 이후 교환완료 확인이 가능합니다.
            - 요청 meetingAt은 offset 포함 형식(예: 2026-05-20T14:30:00+09:00), 응답 meetingAt은 UTC Z 형식(예: 2026-05-20T05:30:00Z)입니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "약속 등록 성공",
                    content = @Content(schema = @Schema(implementation = MeetingResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "직접교환 그룹이 아니거나 교환 단계가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "HOST가 아니거나 그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹 또는 장소를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 약속이 존재함")
    })
    ApiResponse<MeetingResponseDTO> createMeeting(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = MeetingRequestDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "default-place 응답 기반 스냅샷 입력",
                                            value = """
                                            {
                                              "placeName": "스타벅스 강남점",
                                              "address": "서울특별시 강남구 강남대로 100",
                                              "x": 127.027621,
                                              "y": 37.497942,
                                              "addressDetail": "2층",
                                              "meetingAt": "2026-05-20T14:30:00+09:00"
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "새 장소 직접 입력",
                                            value = """
                                            {
                                              "placeName": "강남역",
                                              "address": "서울특별시 강남구 강남대로 396",
                                              "x": 127.027621,
                                              "y": 37.497942,
                                              "addressDetail": "11번 출구 앞",
                                              "meetingAt": "2026-05-20T14:30:00+09:00"
                                            }
                                            """
                                    )
                            }
                    )
            )
            @RequestBody @Valid MeetingRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping
    @Operation(
            summary = "직접 교환 약속 수정",
            description = """
            HOST가 groupId 기준으로 현재 등록된 직접교환 약속을 수정합니다.
            
            - 프론트가 meetingId를 들고 있지 않아도 됩니다.
            - 수정 가능 필드: placeName, address, zipCode, x, y, addressDetail, meetingAt
            - 요청 meetingAt은 offset 포함 형식(예: 2026-05-20T14:30:00+09:00), 응답 meetingAt은 UTC Z 형식(예: 2026-05-20T05:30:00Z)입니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 수정 성공",
                    content = @Content(schema = @Schema(implementation = MeetingResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "직접교환 그룹이 아니거나 교환 단계가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "HOST가 아니거나 그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속 또는 장소를 찾을 수 없음")
    })
    ApiResponse<MeetingResponseDTO> updateMeeting(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = MeetingRequestDTO.class),
                            examples = {
                                    @ExampleObject(
                                            name = "default-place 응답 기반 스냅샷 입력",
                                            value = """
                                            {
                                              "placeName": "스타벅스 강남점",
                                              "address": "서울특별시 강남구 강남대로 100",
                                              "x": 127.027621,
                                              "y": 37.497942,
                                              "addressDetail": "2층",
                                              "meetingAt": "2026-05-20T14:30:00+09:00"
                                            }
                                            """
                                    ),
                                    @ExampleObject(
                                            name = "새 장소 직접 입력",
                                            value = """
                                            {
                                              "placeName": "강남역",
                                              "address": "서울특별시 강남구 강남대로 396",
                                              "x": 127.027621,
                                              "y": 37.497942,
                                              "addressDetail": "11번 출구 앞",
                                              "meetingAt": "2026-05-20T14:30:00+09:00"
                                            }
                                            """
                                    )
                            }
                    )
            )
            @RequestBody @Valid MeetingRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @GetMapping
    @Operation(
            summary = "직접 교환 약속 상세 조회",
            description = """
            HOST/GUEST가 groupId 기준으로 등록된 약속 정보를 조회합니다.
            
            - 조회 전 현재 로그인 사용자가 교환 단계(EXCHANGING 또는 RETURNING)인지 먼저 검증합니다.
            - 교환완료 버튼 활성화 여부는 별도로 계산하지 않습니다.
            - 응답 meetingAt은 UTC Z 형식(예: 2026-05-20T05:30:00Z)입니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 조회 성공",
                    content = @Content(schema = @Schema(implementation = MeetingResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "직접교환 그룹이 아니거나 교환 단계가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속을 찾을 수 없음")
    })
    ApiResponse<MeetingResponseDTO> getMeeting(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @GetMapping("/default-place")
    @Operation(
            summary = "직접 교환 기본 장소 조회",
            description = """
            그룹 생성 시 선택한 희망교환장소를 약속 등록 기본값으로 조회합니다.
            
            - 직접교환 그룹에서만 가능합니다.
            - 그룹 멤버만 조회할 수 있습니다.
            - 그룹 선택 장소가 배송지인 경우 기본 장소로 사용할 수 없습니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "기본 장소 조회 성공",
                    content = @Content(schema = @Schema(implementation = MeetingDefaultPlaceResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "직접교환 그룹이 아니거나 선택 장소가 부적합함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹 또는 선택 장소를 찾을 수 없음")
    })
    ApiResponse<MeetingDefaultPlaceResponseDTO> getDefaultPlace(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/completion")
    @Operation(
            summary = "직접 교환 완료 확인",
            description = """
            HOST/GUEST가 각각 직접 교환 완료를 확인합니다.
            
            - 약속 시간이 아직 지나지 않아도 완료 처리 가능합니다.
            - 한 명만 완료하면 상대방 확인을 기다립니다.
            - 두 명 모두 완료하면 1차 교환 단계에서는 PARTNER_BOOK_READING으로 전환됩니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "교환 완료 확인 성공",
                    content = @Content(schema = @Schema(implementation = MeetingResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "직접교환 그룹이 아니거나 교환 완료 가능 단계가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 교환 완료 처리됨")
    })
    ApiResponse<MeetingResponseDTO> completeMeeting(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );
}
