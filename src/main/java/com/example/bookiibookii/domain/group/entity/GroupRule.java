package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.user.enums.Tag;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "group_rule")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @Column(name = "rule_content", nullable = false)
    private String ruleContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "tag", nullable = false)
    private Tag tag;

    public static GroupRule create(Groups group, String ruleContent, Tag tag) {
        return GroupRule.builder()
                .group(group)
                .ruleContent(ruleContent)
                .tag(tag)
                .build();
    }
}
