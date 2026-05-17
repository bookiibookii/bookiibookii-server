package com.example.bookiibookii.domain.memberbook.repository;

import com.example.bookiibookii.domain.memberbook.entity.MemberCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface MemberCardRepository extends JpaRepository<MemberCard, Long> {

    @Query("""
        SELECT mc.card.id FROM MemberCard mc
        JOIN mc.matchedMember mm
        WHERE mm.user.id = :userId AND mc.hidden = true
        """)
    List<Long> findHiddenCardIdsByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT mc.card.id FROM MemberCard mc
        JOIN mc.matchedMember mm
        WHERE mm.user.id = :userId AND mm.group.groupId = :groupId AND mc.hidden = true
        """)
    List<Long> findHiddenCardIdsByUserIdAndGroupId(
            @Param("userId") Long userId,
            @Param("groupId") Long groupId
    );

    Optional<MemberCard> findByMatchedMember_IdAndCard_Id(Long matchedMemberId, Long cardId);

    @Query("""
        SELECT mc.card.id FROM MemberCard mc
        JOIN mc.matchedMember mm
        WHERE mm.user.id = :userId AND mc.card.id IN :cardIds AND mc.bookmarked = true
        """)
    Set<Long> findBookmarkedCardIdsByUserIdAndCardIdIn(
            @Param("userId") Long userId,
            @Param("cardIds") List<Long> cardIds
    );

    @Query("""
        SELECT mc FROM MemberCard mc
        JOIN mc.matchedMember mm
        WHERE mm.user.id = :userId AND mc.card.id = :cardId
        """)
    Optional<MemberCard> findByUserIdAndCardId(
            @Param("userId") Long userId,
            @Param("cardId") Long cardId
    );

    /** 북마크 목록용: bookmarked = true, hidden = false 인 MemberCard + card·image·book·작성자 fetch */
    @Query("""
        SELECT mc FROM MemberCard mc
        JOIN FETCH mc.matchedMember mm
        JOIN FETCH mc.card c
        LEFT JOIN FETCH c.cardImages
        JOIN FETCH c.memberBook mb
        JOIN FETCH mb.book
        JOIN FETCH mb.matchedMember creatorMm
        JOIN FETCH creatorMm.user u
        LEFT JOIN FETCH u.userImage
        WHERE mm.user.id = :userId AND mc.bookmarked = true AND mc.hidden = false
        ORDER BY mc.createdAt DESC
        """)
    List<MemberCard> findByUserIdAndBookmarkedTrueWithCardDetailsOrderByCreatedAtDesc(
            @Param("userId") Long userId
    );
}
