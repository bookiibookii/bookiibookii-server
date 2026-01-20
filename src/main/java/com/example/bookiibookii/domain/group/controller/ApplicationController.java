package com.example.bookiibookii.domain.group.controller;

import com.example.bookiibookii.domain.group.dto.req.ApplicationRequestDTO;
import com.example.bookiibookii.domain.group.dto.res.ApplicationResponseDTO;
import com.example.bookiibookii.domain.group.service.ApplicationService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class ApplicationController implements ApplicationControllerDocs {

    private final ApplicationService applicationService;

    @GetMapping("/{groupId}/applylist")
    public ApiResponse<ApplicationResponseDTO.ApplicationListDTO> getApplicantList(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        ApplicationResponseDTO.ApplicationListDTO response = applicationService.getApplicantList(groupId, user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response);
    }

    @PatchMapping("/apply/{applyId}")
    public ApiResponse<ApplicationResponseDTO.UpdateResultDTO> updateApplicationStatus(
            @PathVariable(name = "applyId") Long applyId,
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody ApplicationRequestDTO.UpdateStatusDTO request
    ) {
        ApplicationResponseDTO.UpdateResultDTO result =
                applicationService.updateApplicationStatus(applyId, user.getId(), request.getStatus());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    @PostMapping("/{groupId}/apply")
    public ApiResponse<ApplicationResponseDTO.JoinResultDTO> joinGroup(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody @Valid ApplicationRequestDTO.JoinApplicationDTO request
    ) {
        ApplicationResponseDTO.JoinResultDTO result = applicationService.joinGroup(groupId, user.getId(), request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }
}