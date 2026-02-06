package com.example.bookiibookii.domain.userbook.entity;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "card_bookmark",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "card_id"})
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardBookmark extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_bookmark_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;
}
