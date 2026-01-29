package com.example.bookiibookii.domain.userbook.repository;

import com.example.bookiibookii.domain.userbook.dto.res.UserBookResponseDTO;
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

    @Query("""
        SELECT b.title
        FROM UserBook ub
        JOIN Book b ON b.id = ub.bookId
        WHERE ub.user.id = :userId
        ORDER BY ub.updatedAt DESC
    """)
    List<String> findRecentBookTitle(@Param("userId") Long userId, Pageable pageable);

    // 완독한 책 개수
    Long countByUser_Id(Long userId);

    // 최근 읽은 책과 평점 조회
    @Query("""
        SELECT new com.example.bookiibookii.domain.userbook.dto.res.UserBookResponseDTO$MypageBookDto(
            b.title, ub.rating
        )
        FROM UserBook ub
        JOIN Book b ON b.id = ub.bookId
        WHERE ub.user.id = :userId
        ORDER BY ub.updatedAt DESC
    """)
    List<UserBookResponseDTO.MypageBookDto> findRecentBooksWithRating(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
