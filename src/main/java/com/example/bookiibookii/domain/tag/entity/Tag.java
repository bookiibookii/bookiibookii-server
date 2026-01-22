package com.example.bookiibookii.domain.tag.entity;

import com.example.bookiibookii.domain.tag.enums.TagType;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tag")
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Tag extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TagType type;

    @Column(nullable = false, length = 20)
    private String code;
}
