package com.example.bookiibookii.domain.policy.dto.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class PolicyRequestDTO {

    public record AgreePolicies(
            @NotEmpty
            List<@Valid AgreePolicyItem> agreements
    ) {}

    public record AgreePolicyItem(
            @NotNull
            Long policyDocumentId,

            @NotNull
            Boolean agreed
    ) {}
}
