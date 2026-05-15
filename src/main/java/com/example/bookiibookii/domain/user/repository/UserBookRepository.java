package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBookRepository extends JpaRepository<UserBook, Long> {
    void deleteAllByUser(User user);
    List<UserBook> findByUser(User user);

    @Query("""
        select new com.example.bookiibookii.domain.user.dto.res.UserResponseDTO$UserBookDto(
            b.title,
            b.author,
            b.image
        )
        from UserBook up
        join up.book b
        where up.user.id = :userId
        order by up.createdAt desc
    """)
    List<UserResponseDTO.UserBookDto> findUserBooks(Long userId);

    @Query("SELECT ub FROM UserBook ub JOIN FETCH ub.book WHERE ub.user.id = :userId AND ub.isFavorite = true")
    List<UserBook> findFavoriteBooksByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT ub FROM UserBook ub
        JOIN FETCH ub.book
        WHERE ub.user.id = :userId
        AND ub.displayOrder IS NOT NULL
        ORDER BY ub.displayOrder ASC
    """)
    List<UserBook> findRepresentativeBooks(@Param("userId") Long userId);

    Optional<UserBook> findByIdAndUser_Id(Long id, Long userId);

    Optional<UserBook> findByUser_IdAndBook_Id(Long userId, Long bookId);

    long countByUser_IdAndIsFavoriteTrue(Long userId);
}
