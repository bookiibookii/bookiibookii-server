package com.example.bookiibookii.domain.notification.entity;

import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "keyword",
        indexes = {
                @Index(name = "ix_keyword_normalized", columnList = "normalized_content"),
                @Index(name = "ix_keyword_prefix2", columnList = "prefix2")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_keyword_normalized", columnNames = "normalized_content")
        }
)
public class Keyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 표시용 원본 유지
    @Column(name = "content", nullable = false, length = 100)
    private String content;

    // 매칭 용도 - 압축
    @Column(name = "normalized_content", nullable = false, length = 100)
    private String normalizedContent;

    // 검색용 앞 두자리
    @Column(name="prefix2", nullable=false, length=2)
    private String prefix2;
}
