package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.dto.req.TrackerMeetingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerReceiveRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.*;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerImageSuccessCode;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerSuccessCode;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
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

    @Override
    @GetMapping("/me/trackers")
    public ApiResponse<List<TrackerListResponseDTO>> getTrackerList(
            @AuthenticationPrincipal(expression = "user") User user) {
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_LIST_GET_OK, trackerService.getTrackerList(user.getId()));
    }

    @Override
    @GetMapping("/me/trackers/host")
    public ApiResponse<List<TrackerListResponseDTO>> getHostTrackers(
            @AuthenticationPrincipal(expression = "user") User user) {
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_HOST_LIST_GET_OK, trackerService.getHostTrackerList(user.getId()));
    }

    @Override
    @GetMapping("/me/trackers/guest")
    public ApiResponse<List<TrackerListResponseDTO>> getGuestTrackers(
            @AuthenticationPrincipal(expression = "user") User user) {
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_GUEST_LIST_GET_OK, trackerService.getGuestTrackerList(user.getId()));
    }

    @Override
    @GetMapping("/{groupId}/tracker")
    public ApiResponse<TrackerDetailResponseDTO> getTrackerDetail(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_DETAIL_GET_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    /*@Override
    @GetMapping("/{groupId}/tracker/histories")
    public ApiResponse<List<TrackerHistoryResponse>> getTrackerHistories(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_HISTORY_GET_OK, trackerService.getTrackerHistoriesByGroupId(groupId, user));
    }*/

    @Override
    @GetMapping("/{groupId}/tracker/check/shipping")
    public ApiResponse<TrackerImageGetResponseDTO> getShippingProofImageUrl(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        TrackerImageGetResponseDTO response = trackerService.getShippingProofImageUrl(groupId, user);
        return ApiResponse.onSuccess(TrackerImageSuccessCode.TRACKING_IMAGE_FOUND, response);
    }

    @Override
    @GetMapping("/{groupId}/tracker/check/received")
    public ApiResponse<TrackerImageGetResponseDTO> getReceivedProofImageUrl(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        TrackerImageGetResponseDTO response = trackerService.getReceivedProofImageUrl(groupId, user);
        return ApiResponse.onSuccess(TrackerImageSuccessCode.RECEIVED_IMAGE_FOUND, response);
    }

    @Override
    @PostMapping("/{groupId}/tracker/images/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForTrackerImage(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        PresignedUrlResponseDTO responseDTO = trackerService.getPresignedPutUrlForTrackerImage(groupId, user);
        return ApiResponse.onSuccess(TrackerImageSuccessCode.TRACKING_PRESIGNED_URL_ISSUED, responseDTO);
    }

    @Override
    @PostMapping("/{groupId}/tracker/shipping")
    public ApiResponse<TrackerDetailResponseDTO> registerShipping(
            @PathVariable Long groupId,
            @RequestBody @Valid TrackerShippingRequestDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.registerShipping(groupId, request, user);
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_SHIPPING_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/receive")
    public ApiResponse<TrackerDetailResponseDTO> registerReceive(
            @PathVariable Long groupId,
            @RequestBody @Valid TrackerReceiveRequestDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.registerReceive(groupId, request, user);
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_RECEIVE_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/reading")
    public ApiResponse<TrackerDetailResponseDTO> registerReading(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.registerReading(groupId, user);
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_READING_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/extension")
    public ApiResponse<TrackerDetailResponseDTO> registerExtension(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "3") int days,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.registerExtensionDays(groupId, days, user);
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_EXTENSION_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/done")
    public ApiResponse<TrackerDetailResponseDTO> registerReadingDone(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.registerReadingDone(groupId, user);
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_DONE_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    @Override
    @GetMapping("/{groupId}/tracker/meeting")
    public ApiResponse<TrackerMeetingResponseDTO> getMeetingDetail(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_MEETING_GET_OK, trackerService.getMeetingDetailByGroupId(groupId, user));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/makeMeeting")
    public ApiResponse<TrackerMeetingResponseDTO> updateMeeting( // 🟢 반환 타입 변경
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


    @Override
    @PatchMapping("/{groupId}/tracker/meeting/complete")
    public ApiResponse<TrackerDetailResponseDTO> completeMeeting(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.completeMeeting(groupId, user);
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_MEETING_DONE_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/confirm-reception")
    public ApiResponse<TrackerDetailResponseDTO> verifyPartnerReception(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.verifyPartnerReception(groupId, user);
        // 승인 후 최신 트래커 상세 정보를 반환합니다.
        return ApiResponse.onSuccess(TrackerSuccessCode.RECEPTION_VERIFIED,trackerService.getTrackerDetailByGroupId(groupId, user)
        );
    }

}
