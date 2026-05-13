package com.example.bookiibookii.domain.policy.controller;

import com.example.bookiibookii.domain.policy.dto.req.PolicyRequestDTO;
import com.example.bookiibookii.domain.policy.dto.res.PolicyResponseDTO;
import com.example.bookiibookii.domain.policy.exception.code.PolicySuccessCode;
import com.example.bookiibookii.domain.policy.service.PolicyAgreementService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/policies")
public class PolicyAgreementController implements PolicyAgreementControllerDocs {

    private final PolicyAgreementService policyService;

    // 현재 유효한 약관 목록 및 내 동의 여부 조회
    @GetMapping("/agreements/me")
    public ApiResponse<PolicyResponseDTO.AgreementStatus> getMyPolicyAgreementStatus(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        return ApiResponse.onSuccess(
                PolicySuccessCode.POLICY_AGREEMENT_STATUS_OK,
                policyService.getMyPolicyAgreementStatus(user.getId())
        );
    }

    @PostMapping("/agreements")
    public ApiResponse<PolicyResponseDTO.AgreePolicies> agreePolicies(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody @Valid PolicyRequestDTO.AgreePolicies request
    ) {
        return ApiResponse.onSuccess(
                PolicySuccessCode.POLICY_AGREE_OK,
                policyService.agreePolicies(user.getId(), request)
        );
    }
}