package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.dto.res.*;
import com.example.bookiibookii.domain.tracker.dto.req.ExtendReadingPeriodReqDTO;
import com.example.bookiibookii.domain.tracker.dto.req.ReadingProgressRequestDTO;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerSuccessCode;
import com.example.bookiibookii.domain.tracker.service.TrackerService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TrackerController implements TrackerControllerDocs {

    private final TrackerService trackerService;

    @GetMapping("/me/trackers")
    public ApiResponse<List<TrackerListItemResDTO>> getTrackerList(
            @AuthenticationPrincipal(expression = "user") User user) {
        return ApiResponse.onSuccess(
                TrackerSuccessCode.TRACKER_LIST_GET_OK,
                trackerService.getTrackerList(user)
        );
    }

    @GetMapping("/trackers/{groupId}/tracker")
    public ApiResponse<TrackerDetailResDTO> getTrackerDetail(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(TrackerSuccessCode.TRACKER_DETAIL_GET_OK, trackerService.getTrackerDetail(groupId, user));
    }

    @PatchMapping("/trackers/{groupId}/reading-progress")
    public ApiResponse<ReadingProgressResponseDTO> updateReadingProgress(
            @PathVariable Long groupId,
            @RequestBody ReadingProgressRequestDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(
                TrackerSuccessCode.TRACKER_READING_PROGRESS_OK,
                trackerService.updateReadingProgress(groupId, request, user)
        );
    }

    @PatchMapping("/trackers/{groupId}/reading-period")
    public ApiResponse<ExtendReadingPeriodResDTO> extendReadingPeriod(
            @PathVariable Long groupId,
            @RequestBody ExtendReadingPeriodReqDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(
                TrackerSuccessCode.TRACKER_READING_PERIOD_EXTENDED_OK,
                trackerService.extendReadingPeriod(groupId, request, user)
        );
    }
}
