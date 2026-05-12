package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
