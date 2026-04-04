package com.example.bookiibookii.domain.group.entity;

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

    @Column(name = "group_tag")
    private String groupTag;

    public static GroupRule create(Groups group, String ruleContent) {
        return GroupRule.builder()
                .group(group)
                .ruleContent(ruleContent)
                .build();
    }
}
