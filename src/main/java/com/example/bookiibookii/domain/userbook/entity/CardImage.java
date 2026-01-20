package com.example.bookiibookii.domain.userbook.entity;

import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "card_image")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CardImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_image_id")
    private Long id;

    @Column(name = "s3_key", length = 255, unique = true, nullable = false)
    private String s3Key;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false, unique = true)
    private Card card;
}
