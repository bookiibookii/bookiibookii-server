package com.example.bookiibookii.domain.policy.repository;

import com.example.bookiibookii.domain.policy.entity.UserPolicyAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserPolicyAgreementRepository extends JpaRepository<UserPolicyAgreement, Long> {

    @Query("""
        SELECT upa
        FROM UserPolicyAgreement upa
        WHERE upa.user.id = :userId
          AND upa.policyDocument.id IN :policyDocumentIds
          AND upa.actedAt = (
              SELECT MAX(upa2.actedAt)
              FROM UserPolicyAgreement upa2
              WHERE upa2.user.id = upa.user.id
                AND upa2.policyDocument.id = upa.policyDocument.id
          )
        """)
    List<UserPolicyAgreement> findLatestByUserIdAndPolicyDocumentIds(
            @Param("userId") Long userId,
            @Param("policyDocumentIds") List<Long> policyDocumentIds
    );
}
