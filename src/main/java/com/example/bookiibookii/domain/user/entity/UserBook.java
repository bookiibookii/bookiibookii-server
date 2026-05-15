package com.example.bookiibookii.domain.user.entity;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "user_book",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "book_id"})
        }
)
public class UserBook extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_book_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "is_favorite", nullable = false)
    private boolean isFavorite;

    @Column(name = "display_order")
    private Integer displayOrder; // 대표책 순서 1~7, null = 대표책 아님

    public static UserBook create(User user, Book book, boolean isFavorite) {
        return UserBook.builder()
                .user(user)
                .book(book)
                .isFavorite(isFavorite)
                .displayOrder(null)
                .build();
    }

    // 온보딩: 인생책 + 대표책 동시 등록
    public static UserBook createFavorite(User user, Book book, Integer displayOrder) {
        return UserBook.builder()
                .user(user)
                .book(book)
                .isFavorite(true)
                .displayOrder(displayOrder)
                .build();
    }

    // 완독책을 대표책으로 등록 (인생책 아님)
    public static UserBook createRepresentative(User user, Book book, Integer displayOrder) {
        return UserBook.builder()
                .user(user)
                .book(book)
                .isFavorite(false)
                .displayOrder(displayOrder)
                .build();
    }

    public void updateDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void updateIsFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }
}
