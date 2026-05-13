package com.example.bookiibookii.domain.policy.dto.res;

import com.example.bookiibookii.domain.policy.enums.PolicyType;

import java.time.LocalDateTime;
import java.util.List;

public class PolicyResponseDTO {

    public record AgreementStatus(
            List<PolicyAgreementStatusItem> policies
    ) {}

    public record PolicyAgreementStatusItem(
            Long policyDocumentId,
            PolicyType type,
            String version,
            String title,
            String content,
            boolean required,
            LocalDateTime effectiveFrom,
            boolean agreed,
            LocalDateTime agreedAt
    ) {}

    public record AgreePolicies(
            List<Long> agreedPolicyDocumentIds
    ) {}
}
