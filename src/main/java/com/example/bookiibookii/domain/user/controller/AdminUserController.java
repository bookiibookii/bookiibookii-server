package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.service.AdminUserService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/admins")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController implements AdminUserControllerDocs {

    private final AdminUserService adminUserService;

    @GetMapping
    public ApiResponse<List<UserResponseDTO.AdminUserListDTO>> getAdminUsers() {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, adminUserService.getAdminUsers());
    }
}
