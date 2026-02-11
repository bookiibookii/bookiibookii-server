package com.example.bookiibookii.domain.comment.entity;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "secret", nullable = false)
    private boolean secret;

    @Column(name = "secret_target_user_id")
    private Long secretTargetUserId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Groups group;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    @Builder.Default
    @OneToMany(mappedBy = "parent", cascade = CascadeType.REMOVE)
    private List<Comment> children = new ArrayList<>();

    public void markDeleted() {
        if (this.deleted) return;
        this.deleted = true;
    }
}
