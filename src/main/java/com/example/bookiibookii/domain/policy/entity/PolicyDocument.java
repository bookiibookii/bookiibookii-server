package com.example.bookiibookii.domain.policy.entity;

import com.example.bookiibookii.domain.policy.enums.PolicyType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
        name = "policy_document",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_policy_document_type_version",
                        columnNames = {"type", "version"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private PolicyType type;

    @Column(name = "version", nullable = false, length = 30)
    private String version;

    @Column(name = "title", nullable = false)
    private String title;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "required", nullable = false)
    private boolean required;

    /**
     * 이 약관 버전이 유효해지는 시점.
     * 현재 노출할 약관 조회 시 effectiveFrom <= now 중 type별 최신 문서를 가져온다.
     */
    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;
}
