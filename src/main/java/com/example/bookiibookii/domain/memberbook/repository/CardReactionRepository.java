package com.example.bookiibookii.domain.memberbook.repository;

import com.example.bookiibookii.domain.memberbook.entity.CardReaction;
import com.example.bookiibookii.domain.memberbook.enums.CardReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardReactionRepository extends JpaRepository<CardReaction, Long> {

    Optional<CardReaction> findByMatchedMember_IdAndCard_IdAndReaction(
            Long matchedMemberId,
            Long cardId,
            CardReactionType reaction
    );

    @Query("""
        SELECT cr.card.id, cr.reaction, COUNT(cr)
        FROM CardReaction cr
        WHERE cr.card.id IN :cardIds
        GROUP BY cr.card.id, cr.reaction
        """)
    List<Object[]> countByCardIdsGroupByReaction(@Param("cardIds") List<Long> cardIds);

    @Query("""
        SELECT cr FROM CardReaction cr
        JOIN cr.matchedMember mm
        WHERE mm.user.id = :userId AND cr.card.id IN :cardIds
        """)
    List<CardReaction> findByUserIdAndCardIdIn(
            @Param("userId") Long userId,
            @Param("cardIds") List<Long> cardIds
    );

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CardReaction cr WHERE cr.matchedMember.id = :matchedMemberId")
    void deleteByMatchedMember_Id(@Param("matchedMemberId") Long matchedMemberId);
}
