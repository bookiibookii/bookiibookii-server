package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.dto.req.TrackerMeetingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerReceiveRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.*;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Tracker", description = "도서 트래킹 관련 API (상태 조회 및 이력 관리)")
public interface TrackerControllerDocs {

    // --- 1. 조회 관련 ---
    @GetMapping("/me/trackers")
    @Operation(
            summary = "나의 트래커 전체 리스트 조회",
            description = """
            나의 모든 트래커(RELAY/TOGETHER)를 조회합니다.
            - RELAY 타입의 relayDetail에는 hostProfileImageUrl(호스트 프로필 이미지 Presigned GET URL)과 guestProfileImageUrls(게스트 프로필 이미지 Presigned GET URL 리스트)가 포함됩니다.
            """
    )
    ApiResponse<List<TrackerListResponseDTO>> getTrackerList(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @GetMapping("/me/trackers/host")
    @Operation(
            summary = "내가 호스트인 트래커 리스트 조회",
            description = """
            내가 호스트로 참여 중인 트래커 목록을 조회합니다.
            - RELAY 타입의 relayDetail에는 hostProfileImageUrl(호스트 프로필 이미지 Presigned GET URL)과 guestProfileImageUrls(게스트 프로필 이미지 Presigned GET URL 리스트)가 포함됩니다.
            """
    )
    ApiResponse<List<TrackerListResponseDTO>> getHostTrackers(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @GetMapping("/me/trackers/guest")
    @Operation(
            summary = "내가 게스트인 트래커 리스트 조회",
            description = """
            내가 게스트로 참여 중인 트래커 목록을 조회합니다.
            - RELAY 타입의 relayDetail에는 hostProfileImageUrl(호스트 프로필 이미지 Presigned GET URL)과 guestProfileImageUrls(게스트 프로필 이미지 Presigned GET URL 리스트)가 포함됩니다.
            """
    )
    ApiResponse<List<TrackerListResponseDTO>> getGuestTrackers(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @GetMapping("/{groupId}/tracker")
    @Operation(summary = "트래킹 상세 현황 조회")
    ApiResponse<TrackerDetailResponseDTO> getTrackerDetail(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    /*@Operation(summary = "트래킹 히스토리(이력) 조회")
    @GetMapping("/{groupId}/tracker/histories")
    ApiResponse<List<TrackerHistoryResponse>> getTrackerHistories(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );*/

    // --- 2. 이미지 관련 ---

    @Operation(summary = "배송 인증 사진 보기", description = "수령한 사람이 배송한 사람이 올린 배송 인증(SENDER_PROOF) 이미지를 조회합니다. Presigned GET URL을 반환하며, 같은 그룹 멤버만 조회 가능합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "배송 인증 이미지 없음", content = @Content)
    })
    @GetMapping("/{groupId}/tracker/images/delivery")
    ApiResponse<TrackerImageGetResponseDTO> getShippingProofImageUrl(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "수령 인증 사진 보기", description = "배송한 사람이 수령한 사람이 올린 수령 인증(RECEIVER_PROOF) 이미지를 조회합니다. Presigned GET URL을 반환하며, 같은 그룹 멤버만 조회 가능합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "수령 인증 이미지 없음", content = @Content)
    })
    @GetMapping("/{groupId}/tracker/images/received")
    ApiResponse<TrackerImageGetResponseDTO> getReceivedProofImageUrl(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "트래커 인증 이미지 업로드용 Presigned URL 발급", description = "배송 인증(SENDER_PROOF) 또는 수령 인증(RECEIVER_PROOF) 이미지를 S3에 업로드하기 위한 Presigned PUT URL을 발급합니다. " +
            "발급된 presignedPutUrl로 PUT 요청 후 받은 s3Key를 배송 시작 등록 또는 도서 수령 완료 API에 전달하세요. URL 유효 시간은 10분입니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Presigned URL 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "해당 그룹 멤버가 아님", content = @Content)
    })
    @PostMapping("/{groupId}/tracker/images/presigned-url")
    ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForTrackerImage(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    // --- 3. 배송/수령/독서 상태  ---
    @PatchMapping("/{groupId}/tracker/reading")
    @Operation(summary = "독서 시작 등록", description = "도서 수령 후 실제 독서를 시작할 때 호출합니다.")
    ApiResponse<TrackerDetailResponseDTO> registerReading(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/{groupId}/tracker/extension")
    @Operation(summary = "독서 기간 연장 신청", description = "현재 주자의 독서 기간을 연장합니다. (최대 1회 가능)")
    ApiResponse<TrackerDetailResponseDTO> registerExtension(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(description = "연장할 일수", example = "3") @RequestParam(defaultValue = "3") int days,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/{groupId}/tracker/done")
    @Operation(summary = "독서 완료 등록", description = "도서를 다 읽었을 때 호출합니다. 이후 배송 등록이 가능해집니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "독서 완료 등록 성공",
                    content = @Content(schema = @Schema(implementation = TrackerDetailResponseDTO.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "독서 완료 등록실패", content = @Content)
    })
    ApiResponse<TrackerDetailResponseDTO> registerReadingDone(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );


    @PostMapping("/{groupId}/tracker/delivery")
    @Operation(summary = "배송 시작 등록", description = "책 읽기를 완료하고 다음 주자에게 배송을 시작할 때 정보를 등록합니다. " +
            "배송 인증 이미지는 Presigned URL로 S3 업로드 후 발급받은 s3Key(형식: image/trackers/{uuid})를 전달하세요. TrackerImage(SENDER_PROOF)로 저장됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배송 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력값(s3Key 형식/중복/S3 미존재)", content = @Content)
    })
    ApiResponse<TrackerDetailResponseDTO> registerShipping(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid TrackerShippingRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );


    @PatchMapping("/{groupId}/tracker/reception")
    @Operation(summary = "도서 수령 완료 처리", description = "배송 중인 도서를 수령했을 때 호출합니다. " +
            "수령 인증 이미지는 Presigned URL로 S3 업로드 후 발급받은 s3Key를 전달하세요. TrackerImage(RECEIVER_PROOF)로 저장됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수령 완료 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력값(s3Key 형식/중복/S3 미존재)", content = @Content)
    })
    ApiResponse<TrackerDetailResponseDTO> registerReceive(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid TrackerReceiveRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/{groupId}/tracker/reception/verification")
    @Operation(summary = "상대방의 수령 인증 사진 확인 (승인)")
    ApiResponse<TrackerDetailResponseDTO> verifyPartnerReception(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    );


    // --- 4. 직접 교환(Meeting) 관련 ---
    @GetMapping("/{groupId}/tracker/meetings")
    @Operation(summary = "직접 교환 약속 상세 조회")
    ApiResponse<TrackerMeetingResponseDTO> getMeetingDetail(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/{groupId}/tracker/meetings")
    @Operation(
            summary = "직접 교환 약속 등록/수정",
            description = """
            직접 교환 시 만날 장소와 시간을 등록하거나 수정합니다.
            
            - **최초 등록 시**: 트래커 상태가 `SHIPPING_TO_GUEST`(전달 시) 또는 `SHIPPING_TO_HOST`(반납 시)로 변경됩니다.
            - **수정 시**: 이미 약속이 있는 경우 기존 정보를 업데이트하며, 상대방의 수락 여부(isConfirmed)가 초기화됩니다.
            - **응답**: 수정된 약속의 상세 정보(`TrackerMeetingResponse`)를 반환합니다.
            """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 등록/수정 성공",
                    content = @Content(schema = @Schema(implementation = TrackerMeetingResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (도서 소유자만 가능)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "트래커 또는 약속 정보를 찾을 수 없음")
    })
    ApiResponse<TrackerMeetingResponseDTO> updateMeeting( // 🟢 반환 타입 변경
                                                          @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
                                                          @RequestBody @Valid TrackerMeetingRequestDTO request,
                                                          @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/{groupId}/tracker/meetings/completion")
    @Operation(
            summary = "직접 교환 완료 확인 (상호 확인)",
            description = "직접 교환 현장에서 책을 주고받은 후 양측(호스트, 게스트)이 각각 완료 버튼을 누릅니다. " +
                    "두 명 모두 확인 시 소유권이 이전되며, 상태가 RECEIVED(전달 시) 또는 RETURNED(반납 시)로 즉시 변경됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "교환 확인 처리 성공",
                    content = @Content(schema = @Schema(implementation = TrackerDetailResponseDTO.class)))
    })
    ApiResponse<TrackerDetailResponseDTO> completeMeeting(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );



}