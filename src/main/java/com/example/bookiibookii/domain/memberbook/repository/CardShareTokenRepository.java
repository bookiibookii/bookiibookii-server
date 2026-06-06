package com.example.bookiibookii.domain.memberbook.repository;

import com.example.bookiibookii.domain.memberbook.entity.CardShareToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardShareTokenRepository extends JpaRepository<CardShareToken, Long> {

    @Query("""
        SELECT t FROM CardShareToken t
        WHERE t.card.id = :cardId
        AND t.revokedAt IS NULL
        """)
    List<CardShareToken> findAllActiveByCardId(@Param("cardId") Long cardId);

    @Query("""
        SELECT t FROM CardShareToken t
        JOIN FETCH t.card c
        LEFT JOIN FETCH c.cardImages
        JOIN FETCH c.memberBook mb
        JOIN FETCH mb.book
        JOIN FETCH mb.matchedMember mm
        JOIN FETCH mm.user u
        WHERE t.token = :token
        AND t.revokedAt IS NULL
        AND c.deletedAt IS NULL
        """)
    Optional<CardShareToken> findActiveByTokenWithCardDetails(@Param("token") String token);
}
