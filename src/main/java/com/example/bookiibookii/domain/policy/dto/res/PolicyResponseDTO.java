package com.example.bookiibookii.domain.policy.dto.res;

import com.example.bookiibookii.domain.policy.enums.PolicyType;

import java.time.Instant;
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
            Instant effectiveFrom,
            boolean agreed,
            Instant agreedAt
    ) {}

    public record AgreePolicies(
            List<Long> agreedPolicyDocumentIds
    ) {}
}
