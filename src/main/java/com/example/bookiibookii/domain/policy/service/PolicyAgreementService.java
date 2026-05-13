package com.example.bookiibookii.domain.policy.service;

import com.example.bookiibookii.domain.policy.dto.req.PolicyRequestDTO;
import com.example.bookiibookii.domain.policy.dto.res.PolicyResponseDTO;
import com.example.bookiibookii.domain.policy.entity.PolicyDocument;
import com.example.bookiibookii.domain.policy.entity.UserPolicyAgreement;
import com.example.bookiibookii.domain.policy.exception.PolicyException;
import com.example.bookiibookii.domain.policy.exception.code.PolicyErrorCode;
import com.example.bookiibookii.domain.policy.repository.PolicyDocumentRepository;
import com.example.bookiibookii.domain.policy.repository.UserPolicyAgreementRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyAgreementService {

    private final PolicyDocumentRepository policyDocumentRepository;
    private final UserPolicyAgreementRepository userPolicyAgreementRepository;
    private final UserRepository userRepository;

    public PolicyResponseDTO.AgreementStatus getMyPolicyAgreementStatus(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        List<PolicyDocument> currentPolicies =
                policyDocumentRepository.findCurrentPolicies(now);

        List<Long> policyDocumentIds = currentPolicies.stream()
                .map(PolicyDocument::getId)
                .toList();

        Map<Long, UserPolicyAgreement> agreementMap =
                userPolicyAgreementRepository
                        .findByUserIdAndPolicyDocumentIdIn(userId, policyDocumentIds)
                        .stream()
                        .collect(Collectors.toMap(
                                agreement -> agreement.getPolicyDocument().getId(),
                                Function.identity()
                        ));

        List<PolicyResponseDTO.PolicyAgreementStatusItem> items = currentPolicies.stream()
                .map(policy -> {
                    UserPolicyAgreement agreement = agreementMap.get(policy.getId());

                    boolean agreed = agreement != null && agreement.isAgreed();
                    LocalDateTime agreedAt = agreement != null
                            ? agreement.getAgreedAt()
                            : null;

                    return new PolicyResponseDTO.PolicyAgreementStatusItem(
                            policy.getId(),
                            policy.getType(),
                            policy.getVersion(),
                            policy.getTitle(),
                            policy.getContent(),
                            policy.isRequired(),
                            policy.getEffectiveFrom(),
                            agreed,
                            agreedAt
                    );
                })
                .toList();

        return new PolicyResponseDTO.AgreementStatus(items);
    }

    @Transactional
    public PolicyResponseDTO.AgreePolicies agreePolicies(
            Long userId,
            PolicyRequestDTO.AgreePolicies request
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PolicyException(PolicyErrorCode.USER_NOT_FOUND));

        if (request.agreements() == null || request.agreements().isEmpty()) {
            throw new PolicyException(PolicyErrorCode.POLICY_AGREEMENT_EMPTY);
        }

        LocalDateTime now = LocalDateTime.now();

        List<PolicyDocument> currentPolicies =
                policyDocumentRepository.findCurrentPolicies(now);

        Map<Long, PolicyDocument> currentPolicyMap = currentPolicies.stream()
                .collect(Collectors.toMap(PolicyDocument::getId, Function.identity()));

        Map<Long, Boolean> requestedAgreementMap = request.agreements().stream()
                .collect(Collectors.toMap(
                        PolicyRequestDTO.AgreePolicyItem::policyDocumentId,
                        PolicyRequestDTO.AgreePolicyItem::agreed,
                        (oldValue, newValue) -> newValue
                ));

        validateOnlyCurrentPoliciesRequested(
                requestedAgreementMap.keySet(),
                currentPolicyMap.keySet()
        );

        validateRequiredPoliciesAgreed(
                currentPolicies,
                requestedAgreementMap
        );

        List<Long> agreedPolicyDocumentIds = new ArrayList<>();

        for (PolicyRequestDTO.AgreePolicyItem item : request.agreements()) {
            PolicyDocument policyDocument = currentPolicyMap.get(item.policyDocumentId());

            UserPolicyAgreement agreement = userPolicyAgreementRepository
                    .findByUserIdAndPolicyDocumentId(userId, item.policyDocumentId())
                    .orElseGet(() -> UserPolicyAgreement.agree(user, policyDocument));

            if (Boolean.TRUE.equals(item.agreed())) {
                agreement.agree();
                agreedPolicyDocumentIds.add(policyDocument.getId());
            } else {
                agreement.disagree();
            }

            userPolicyAgreementRepository.save(agreement);
        }

        return new PolicyResponseDTO.AgreePolicies(agreedPolicyDocumentIds);
    }

    // 내부 메서드
    private void validateOnlyCurrentPoliciesRequested(
            Set<Long> requestedPolicyIds,
            Set<Long> currentPolicyIds
    ) {
        boolean hasInvalidPolicy = requestedPolicyIds.stream()
                .anyMatch(policyId -> !currentPolicyIds.contains(policyId));

        if (hasInvalidPolicy) {
            throw new PolicyException(PolicyErrorCode.INVALID_POLICY_INCLUDED);
        }
    }

    private void validateRequiredPoliciesAgreed(
            List<PolicyDocument> currentPolicies,
            Map<Long, Boolean> requestedAgreementMap
    ) {
        for (PolicyDocument policy : currentPolicies) {
            if (!policy.isRequired()) {
                continue;
            }

            Boolean agreed = requestedAgreementMap.get(policy.getId());

            if (!Boolean.TRUE.equals(agreed)) {
                throw new PolicyException(PolicyErrorCode.REQUIRED_POLICY_NOT_AGREED);
            }
        }
    }
}
