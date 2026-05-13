package com.example.bookiibookii.domain.policy.repository;

import com.example.bookiibookii.domain.policy.entity.UserPolicyAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserPolicyAgreementRepository extends JpaRepository<UserPolicyAgreement, Long> {

    List<UserPolicyAgreement> findByUserIdAndPolicyDocumentIdIn(
            Long userId,
            Collection<Long> policyDocumentIds
    );

    Optional<UserPolicyAgreement> findByUserIdAndPolicyDocumentId(
            Long userId,
            Long policyDocumentId
    );
}
