package com.example.bookiibookii.domain.policy.repository;

import com.example.bookiibookii.domain.policy.entity.PolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, Long> {

    @Query("""
        select p
        from PolicyDocument p
        where p.effectiveFrom = (
            select max(p2.effectiveFrom)
            from PolicyDocument p2
            where p2.type = p.type
              and p2.effectiveFrom <= :now
        )
        order by p.required desc, p.type asc
    """)
    List<PolicyDocument> findCurrentPolicies(@Param("now") LocalDateTime now);

    List<PolicyDocument> findByIdIn(Collection<Long> ids);
}