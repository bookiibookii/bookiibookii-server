package com.example.bookiibookii.domain.book.repository;

import com.example.bookiibookii.domain.book.entity.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn13(String isbn13);

    @Modifying
    @Query(value = """
            INSERT INTO book (isbn13, title, author, publisher, image, total_pages, link, category, created_at, updated_at)
            VALUES (:#{#book.isbn13}, :#{#book.title}, :#{#book.author}, :#{#book.publisher}, :#{#book.image},
                    :#{#book.totalPages}, :#{#book.link}, :category, :now, :now)
            ON DUPLICATE KEY UPDATE book_id = book_id
            """, nativeQuery = true)
    void insertIfAbsent(@Param("book") Book book, @Param("category") String category,
                        @Param("now") Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.isbn13 = :isbn")
    Optional<Book> findCurrentByIsbn13(@Param("isbn") String isbn);
}
