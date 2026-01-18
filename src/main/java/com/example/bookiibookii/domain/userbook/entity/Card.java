package com.example.bookiibookii.domain.userbook.entity;

import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL)
    private List<CardImage> cardImages = new ArrayList<>();
}
