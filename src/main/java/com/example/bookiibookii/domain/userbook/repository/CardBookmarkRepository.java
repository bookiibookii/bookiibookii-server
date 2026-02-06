package com.example.bookiibookii.domain.userbook.repository;

import com.example.bookiibookii.domain.userbook.entity.CardBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface CardBookmarkRepository extends JpaRepository<CardBookmark, Long> {

    boolean existsByUser_IdAndCard_Id(Long userId, Long cardId);

    Optional<CardBookmark> findByUser_IdAndCard_Id(Long userId, Long cardId);

    @Query("SELECT cb.card.id FROM CardBookmark cb WHERE cb.user.id = :userId AND cb.card.id IN :cardIds")
    Set<Long> findBookmarkedCardIdsByUserIdAndCardIdIn(@Param("userId") Long userId, @Param("cardIds") List<Long> cardIds);

    @Query("SELECT cb FROM CardBookmark cb JOIN FETCH cb.card c LEFT JOIN FETCH c.cardImage LEFT JOIN FETCH c.userBook ub LEFT JOIN FETCH ub.group g LEFT JOIN FETCH g.book WHERE cb.user.id = :userId ORDER BY cb.createdAt DESC")
    List<CardBookmark> findByUser_IdWithCardAndImageAndBookOrderByCreatedAtDesc(@Param("userId") Long userId);
}
