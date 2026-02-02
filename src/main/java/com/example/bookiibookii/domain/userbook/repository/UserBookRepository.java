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

    // 완독한 책 개수
    Long countByUser_Id(Long userId);

    // 최근 읽은 책과 평점 조회
    @Query("""
        SELECT new com.example.bookiibookii.domain.userbook.dto.res.UserBookResponseDTO$MypageBookDto(
            g.book.title, ub.rating
        )
        FROM UserBook ub
        JOIN ub.group g
        WHERE ub.user.id = :userId
        ORDER BY ub.updatedAt DESC
    """)
    List<UserBookResponseDTO.MypageBookDto> findRecentBooksWithRating(
            @Param("userId") Long userId,
            Pageable pageable
    );

    //검색로직
    @Query("""
    SELECT DISTINCT ub FROM UserBook ub
    JOIN FETCH ub.group g
    JOIN FETCH g.book b
    JOIN FETCH g.host h
    LEFT JOIN FETCH h.userImage
    WHERE ub.user.id = :userId
    AND (b.title LIKE %:keyword% 
         OR b.author LIKE %:keyword% 
         OR ub.comment LIKE %:keyword%)
    ORDER BY ub.updatedAt DESC
    """)
    List<UserBook> searchMyLibrary(@Param("userId") Long userId, @Param("keyword") String keyword);
}
