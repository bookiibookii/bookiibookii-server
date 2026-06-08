package com.example.bookiibookii.domain.aladin.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "bestseller_isbn",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_bestseller_isbn_isbn13", columnNames = "isbn13")
        }
)
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

    @Column(name = "ranking", nullable = false)
    private Integer rank;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(name = "book_image", nullable = false)
    private String bookImage;
}
