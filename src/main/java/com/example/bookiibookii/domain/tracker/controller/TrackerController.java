package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.dto.req.TrackerMeetingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.*;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerImageSuccessCode;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerSuccessCode;
import com.example.bookiibookii.domain.groupbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.tracker.service.TrackerService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class TrackerController implements TrackerControllerDocs {

    private final TrackerService trackerService;

    @GetMapping("/me/trackers")
    public ApiResponse<List<TrackerListResponseDTO>> getTrackerList(
            @AuthenticationPrincipal(expression = "user") User user) {
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_LIST_GET_OK, trackerService.getTrackerList(user.getId()));
    }

    // --- 트래커 상세 조회 ---
    @GetMapping("/{groupId}/tracker")
    public ApiResponse<TrackerDetailResponseDTO> getTrackerDetail(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_DETAIL_GET_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }


    // --- 이미지 관련 (Proof/Presigned) ---
    @GetMapping("/{groupId}/tracker/images/delivery")
    public ApiResponse<TrackerImageGetResponseDTO> getShippingProofImageUrl(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        TrackerImageGetResponseDTO response = trackerService.getShippingProofImageUrl(groupId, user);
        return ApiResponse.onSuccess(TrackerImageSuccessCode.TRACKING_IMAGE_FOUND, response);
    }

    @PostMapping("/{groupId}/tracker/images/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForTrackerImage(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        PresignedUrlResponseDTO responseDTO = trackerService.getPresignedPutUrlForTrackerImage(groupId, user);
        return ApiResponse.onSuccess(TrackerImageSuccessCode.TRACKING_PRESIGNED_URL_ISSUED, responseDTO);
    }

    // --- 배송 및 수령 관련 ---
    @PostMapping("/{groupId}/tracker/delivery")
    public ApiResponse<TrackerDetailResponseDTO> registerShipping(
            @PathVariable Long groupId,
            @RequestBody @Valid TrackerShippingRequestDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.registerShipping(groupId, request, user);
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_SHIPPING_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    // todo : 이미지 없는 수령 완료 처리 추가

    // --- 직접 교환(Meeting) 관련 ---
    public ApiResponse<TrackerMeetingResponseDTO> getMeetingDetail(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_MEETING_GET_OK, trackerService.getMeetingDetailByGroupId(groupId, user));
    }


    public ApiResponse<TrackerMeetingResponseDTO> updateMeeting( //  반환 타입 변경
                                                                 @PathVariable Long groupId,
                                                                 @RequestBody @Valid TrackerMeetingRequestDTO request,
                                                                 @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    ) {
        // 1. 약속 정보 업데이트 수행
        trackerService.updateMeeting(groupId, request, user);

        // 2. 업데이트된 약속 상세 정보를 조회하여 반환
        return ApiResponse.onSuccess(
                TrackerSuccessCode.TRACKER_MEETING_UPDATE_OK,
                trackerService.getMeetingDetailByGroupId(groupId, user) // 🟢 Meeting 정보 반환
        );
    }

    public ApiResponse<TrackerDetailResponseDTO> completeMeeting(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.completeMeeting(groupId, user);
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_MEETING_DONE_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }


}
