package com.example.bookiibookii.domain.userbook.repository;

import com.example.bookiibookii.domain.userbook.entity.UserBook;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBookRepository extends JpaRepository<UserBook, Long> {
    Optional<UserBook> findById(Long id);

    Optional<UserBook> findByIdAndUser_Id(Long id, Long userId);

    @Query("""
        SELECT DISTINCT ub FROM UserBook ub
        JOIN FETCH ub.group g
        JOIN FETCH g.book
        JOIN FETCH g.host h
        LEFT JOIN FETCH h.userImage
        WHERE ub.user.id = :userId
        ORDER BY ub.updatedAt DESC
        """)
    List<UserBook> findAllByUser_IdWithGroupAndBookAndHost(@Param("userId") Long userId);

    @Query("""
        SELECT g.book.title
        FROM UserBook ub
        JOIN ub.group g
        WHERE ub.user.id = :userId
        ORDER BY ub.updatedAt DESC
    """)
    List<String> findRecentBookTitle(@Param("userId") Long userId, Pageable pageable);

}
