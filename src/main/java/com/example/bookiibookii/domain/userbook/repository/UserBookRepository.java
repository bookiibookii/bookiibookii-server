package com.example.bookiibookii.domain.userbook.repository;

import com.example.bookiibookii.domain.userbook.entity.UserBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBookRepository extends JpaRepository<UserBook, Long> {
    Optional<UserBook> findById(Long id);

    @Query("""
        SELECT b.title
        FROM UserBook ub
        JOIN Book b ON b.id = ub.bookId
        WHERE ub.user.id = :userId
        ORDER BY ub.createdAt DESC
    """)
    Optional<String> findRecentBookTitle(Long userId);
}
