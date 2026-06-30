package com.example.bookiibookii.domain.group.controller;

import com.example.bookiibookii.domain.group.service.AdminGroupService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/groups")
@PreAuthorize("hasRole('ADMIN')")
public class AdminGroupController implements AdminGroupControllerDocs {

    private final AdminGroupService adminGroupService;

    @PatchMapping("/{groupId}/force-close")
    public ApiResponse<Void> forceCloseGroup(@PathVariable Long groupId) {
        adminGroupService.forceCloseGroup(groupId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}
