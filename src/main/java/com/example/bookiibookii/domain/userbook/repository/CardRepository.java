package com.example.bookiibookii.domain.userbook.repository;

import com.example.bookiibookii.domain.userbook.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    Optional<Card> findById(Long id);

    @Query("SELECT c FROM Card c JOIN FETCH c.userBook ub JOIN FETCH ub.user JOIN FETCH c.group WHERE c.id = :cardId")
    Optional<Card> findByIdWithUserBookAndGroup(@Param("cardId") Long cardId);

    @Query("SELECT c FROM Card c JOIN FETCH c.cardImage WHERE c.id = :cardId")
    Optional<Card> findByIdWithCardImage(@Param("cardId") Long cardId);

    @Query("SELECT c FROM Card c JOIN FETCH c.cardImage JOIN FETCH c.userBook ub JOIN FETCH ub.user JOIN FETCH ub.group g JOIN FETCH g.book WHERE c.id = :cardId")
    Optional<Card> findByIdWithCardImageAndUserBookAndBook(@Param("cardId") Long cardId);

    @Query("SELECT c FROM Card c JOIN FETCH c.cardImage WHERE c.userBook.id = :userBookId ORDER BY c.createdAt ASC")
    List<Card> findByUserBookIdWithCardImage(@Param("userBookId") Long userBookId);
}
