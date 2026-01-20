package com.example.bookiibookii.domain.userbook.entity;

import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "card")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Card extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_book_id", nullable = false)
    private UserBook userBook;

    @Column(name = "page")
    private Integer page;

    @Column(name = "memo", length = 255)
    private String memo;

    @OneToOne(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    private CardImage cardImage;
}
