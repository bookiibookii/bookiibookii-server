package com.example.bookiibookii.domain.user.entity;

import com.example.bookiibookii.domain.tag.entity.Tag;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "user_tag")
public class UserTag extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "score", nullable = false)
    private Integer score; // METHOD, VIBE Type의 tag별 누적도

    public static UserTag create(User user, Tag tag) {
        return UserTag.builder()
                .user(user)
                .tag(tag)
                .score(0)
                .build();
    }
}
