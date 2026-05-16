package com.example.bookiibookii.domain.memberbook.repository;

import com.example.bookiibookii.domain.memberbook.entity.Cards;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardsRepository extends JpaRepository<Cards, Long> {

    Optional<Cards> findTopByMemberBook_IdOrderByPageDesc(Long memberBookId);

    @Query("""
        SELECT c FROM Cards c
        JOIN FETCH c.memberBook mb
        JOIN FETCH mb.matchedMember mm
        JOIN FETCH mb.book
        LEFT JOIN FETCH c.cardImages
        WHERE c.id = :cardId AND mm.user.id = :userId
        """)
    Optional<Cards> findByIdAndOwnerUserId(
            @Param("cardId") Long cardId,
            @Param("userId") Long userId
    );

    @Query("""
        SELECT c FROM Cards c
        LEFT JOIN FETCH c.cardImages
        JOIN FETCH c.memberBook mb
        JOIN FETCH mb.book
        JOIN FETCH mb.matchedMember mm
        JOIN FETCH mm.user
        WHERE mb.group.groupId = :groupId
        ORDER BY c.createdAt ASC
        """)
    List<Cards> findByGroupIdWithMemberBookAndBookAndCreator(
            @Param("groupId") Long groupId
    );

    @Query("""
        SELECT c FROM Cards c
        LEFT JOIN FETCH c.cardImages
        JOIN FETCH c.memberBook mb
        JOIN FETCH mb.book
        JOIN FETCH mb.matchedMember mm
        JOIN FETCH mm.user
        JOIN FETCH mb.group
        WHERE c.id = :cardId
        """)
    Optional<Cards> findByIdWithDetails(@Param("cardId") Long cardId);
}
