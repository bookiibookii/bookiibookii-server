package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.dto.req.MeetingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.MeetingDefaultPlaceResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.MeetingResponseDTO;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerSuccessCode;
import com.example.bookiibookii.domain.tracker.service.MeetingService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/meetings")
@RequiredArgsConstructor
public class MeetingController implements MeetingControllerDocs {

    private final MeetingService meetingService;

    @PostMapping
    public ApiResponse<MeetingResponseDTO> createMeeting(
            @PathVariable Long groupId,
            @RequestBody @Valid MeetingRequestDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(
                TrackerSuccessCode.TRACKER_MEETING_CREATE_OK,
                meetingService.createMeeting(groupId, request, user)
        );
    }

    @PatchMapping
    public ApiResponse<MeetingResponseDTO> updateMeeting(
            @PathVariable Long groupId,
            @RequestBody @Valid MeetingRequestDTO request,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(
                TrackerSuccessCode.TRACKER_MEETING_UPDATE_OK,
                meetingService.updateMeeting(groupId, request, user)
        );
    }

    @GetMapping
    public ApiResponse<MeetingResponseDTO> getMeeting(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(
                TrackerSuccessCode.TRACKER_MEETING_GET_OK,
                meetingService.getMeeting(groupId, user)
        );
    }

    @GetMapping("/default-place")
    public ApiResponse<MeetingDefaultPlaceResponseDTO> getDefaultPlace(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(
                TrackerSuccessCode.TRACKER_MEETING_DEFAULT_PLACE_GET_OK,
                meetingService.getDefaultPlace(groupId, user)
        );
    }

    @PatchMapping("/completion")
    public ApiResponse<MeetingResponseDTO> completeMeeting(
            @PathVariable Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(
                TrackerSuccessCode.TRACKER_MEETING_DONE_OK,
                meetingService.completeMeeting(groupId, user)
        );
    }
}
