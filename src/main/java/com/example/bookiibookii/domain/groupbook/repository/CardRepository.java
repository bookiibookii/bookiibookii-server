package com.example.bookiibookii.domain.groupbook.repository;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.groupbook.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findById(Long id);

    @Query("SELECT c FROM Card c JOIN FETCH c.groupBook gb JOIN FETCH gb.user JOIN FETCH c.group WHERE c.id = :cardId")
    Optional<Card> findByIdWithGroupBookAndGroup(@Param("cardId") Long cardId);

    @Query("SELECT c FROM Card c JOIN FETCH c.cardImage WHERE c.id = :cardId")
    Optional<Card> findByIdWithCardImage(@Param("cardId") Long cardId);

    @Query("SELECT c FROM Card c JOIN FETCH c.cardImage JOIN FETCH c.groupBook gb JOIN FETCH gb.user JOIN FETCH gb.group g JOIN FETCH g.book WHERE c.id = :cardId")
    Optional<Card> findByIdWithCardImageAndGroupBookAndBook(@Param("cardId") Long cardId);

    @Query("SELECT c FROM Card c JOIN FETCH c.cardImage WHERE c.groupBook.id = :groupBookId ORDER BY c.createdAt ASC")
    List<Card> findByGroupBookIdWithCardImage(@Param("groupBookId") Long groupBookId);

    @Query("SELECT c FROM Card c JOIN FETCH c.cardImage JOIN FETCH c.groupBook gb JOIN FETCH gb.user JOIN FETCH c.group g JOIN FETCH g.book WHERE c.group.groupId = :groupId ORDER BY c.createdAt ASC")
    List<Card> findByGroup_GroupIdWithCardImageAndGroupBookAndGroupAndBook(@Param("groupId") Long groupId);

    Optional<Card> findTopByGroupBook_User_IdAndGroupOrderByPageDesc(Long userId, Groups group);
}
