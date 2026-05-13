package com.example.bookiibookii.domain.policy.controller;

import com.example.bookiibookii.domain.policy.dto.req.PolicyRequestDTO;
import com.example.bookiibookii.domain.policy.dto.res.PolicyResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Policy Agreement", description = "약관 동의 API")
public interface PolicyAgreementControllerDocs {

    @Operation(
            summary = "현재 유효한 약관 목록 및 내 동의 여부 조회",
            description = """
                    현재 시점에 유효한 약관 목록과 사용자의 동의 여부를 조회합니다.
                    
                    동의 이력이 없는 신규 사용자는 각 약관에 대해 agreed=false, agreedAt=null로 응답됩니다.
                    """
    )
    ApiResponse<PolicyResponseDTO.AgreementStatus> getMyPolicyAgreementStatus(
            @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "약관 동의 처리",
            description = """
                    사용자가 약관에 동의 또는 비동의합니다.
                    
                    필수 약관은 반드시 agreed=true여야 하며,
                    선택 약관은 agreed=false가 가능합니다.
                    """
    )
    ApiResponse<PolicyResponseDTO.AgreePolicies> agreePolicies(
            @Parameter(hidden = true)
            @AuthenticationPrincipal(expression = "user") User user,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "약관 동의 요청",
                    content = @Content(
                            schema = @Schema(implementation = PolicyRequestDTO.AgreePolicies.class),
                            examples = @ExampleObject(
                                    name = "약관 동의 요청 예시",
                                    value = """
                                            {
                                              "agreements": [
                                                {
                                                  "policyDocumentId": 1,
                                                  "agreed": true
                                                },
                                                {
                                                  "policyDocumentId": 2,
                                                  "agreed": true
                                                },
                                                {
                                                  "policyDocumentId": 3,
                                                  "agreed": false
                                                }
                                              ]
                                            }
                                            """
                            )
                    )
            )
            @org.springframework.web.bind.annotation.RequestBody
            @Valid
            PolicyRequestDTO.AgreePolicies request
    );
}