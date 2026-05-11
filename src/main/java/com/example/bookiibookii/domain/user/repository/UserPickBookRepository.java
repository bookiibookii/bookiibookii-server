package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserPickBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPickBookRepository extends JpaRepository<UserPickBook, Long> {
    void deleteAllByUser(User user);
    List<UserPickBook> findByUser(User user);

    @Query("""
        select new com.example.bookiibookii.domain.user.dto.res.UserResponseDTO$UserPickBookDto(
            b.title,
            b.author,
            b.image
        )
        from UserPickBook up
        join up.book b
        where up.user.id = :userId
        order by up.createdAt desc
    """)
    List<UserResponseDTO.UserPickBookDto> findUserPickBooks(Long userId);
}
