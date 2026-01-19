package com.example.bookiibookii.domain.tag.entity;

import com.example.bookiibookii.domain.tag.enums.TagCode;
import com.example.bookiibookii.domain.tag.enums.TagType;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

@Entity
@Table(name = "tag",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_tag_type_code", columnNames = {"type", "code"})
        })
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Filter(name = "activeUserFilter", condition = "status = 'ACTIVE'")
@FilterDef(name = "activeUserFilter", defaultCondition = "status = 'ACTIVE'")
public class Tag extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TagType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TagCode code;
}
