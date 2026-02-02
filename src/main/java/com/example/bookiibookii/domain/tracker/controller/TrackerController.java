package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.controller.docs.TrackerApi;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerMeetingRequest;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequest;
import com.example.bookiibookii.domain.tracker.dto.res.*;
import com.example.bookiibookii.domain.tracker.service.TrackerService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class TrackerController implements TrackerApi {

    private final TrackerService trackerService;

    @Override
    @GetMapping("/me/trackers")
    public ApiResponse<List<TrackerListResponse>> getTrackerList(
            @AuthenticationPrincipal(expression = "user") User user) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, trackerService.getTrackerList(user.getId()));
    }

    @Override
    @GetMapping("/me/trackers/host")
    public ApiResponse<List<TrackerListResponse>> getHostTrackers(
            @AuthenticationPrincipal(expression = "user") User user) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, trackerService.getHostTrackerList(user.getId()));
    }

    @Override
    @GetMapping("/me/trackers/guest")
    public ApiResponse<List<TrackerListResponse>> getGuestTrackers(
            @AuthenticationPrincipal(expression = "user") User user) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, trackerService.getGuestTrackerList(user.getId()));
    }

    @Override
    @GetMapping("/{groupId}/tracker")
    public ApiResponse<TrackerDetailResponse> getTrackerDetail(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    @Override
    @GetMapping("/{groupId}/tracker/histories")
    public ApiResponse<List<TrackerHistoryResponse>> getTrackerHistories(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, trackerService.getTrackerHistoriesByGroupId(groupId, user));
    }

    @Override
    @PostMapping("/{groupId}/tracker/shipping")
    public ApiResponse<TrackerDetailResponse> registerShipping(
            @PathVariable Long groupId,
            @RequestBody @Valid TrackerShippingRequest request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.registerShipping(groupId, request, user);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/receive")
    public ApiResponse<TrackerDetailResponse> registerReceive(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.registerReceive(groupId, user);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/reading")
    public ApiResponse<TrackerDetailResponse> registerReading(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.registerReading(groupId, user);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/extension")
    public ApiResponse<TrackerDetailResponse> registerExtension(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "3") int days,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.registerExtensionDays(groupId, days, user);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/done")
    public ApiResponse<TrackerDetailResponse> registerReadingDone(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.registerReadingDone(groupId, user);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }

    @Override
    @GetMapping("/{groupId}/tracker/meeting")
    public ApiResponse<TrackerMeetingResponse> getMeetingDetail(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, trackerService.getMeetingDetailByGroupId(groupId, user));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/makeMeeting")
    public ApiResponse<TrackerDetailResponse> updateMeeting(
            @PathVariable Long groupId,
            @RequestBody @Valid TrackerMeetingRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    ) {
        trackerService.updateMeeting(groupId, request, user);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, trackerService.getTrackerDetailByGroupId(groupId, user));
    }
}