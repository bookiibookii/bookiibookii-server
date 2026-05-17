package com.example.bookiibookii.domain.aladin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bestseller_isbn")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BestsellerIsbn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 13)
    private String isbn13;

    @Column(nullable = false)
    private Integer rank;
}
