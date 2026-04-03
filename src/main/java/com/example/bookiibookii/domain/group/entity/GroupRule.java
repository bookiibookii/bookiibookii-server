package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.user.enums.Tag;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupRule extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Groups group;

    @Column(name = "rule_content", length = 200)
    private String ruleContent;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_tag", length = 20)
    private Tag tag;

    public static GroupRule create(Groups group, Tag tag, String ruleContent) {
        return GroupRule.builder()
                .group(group)
                .tag(tag)
                .ruleContent(ruleContent)
                .build();
    }
}
