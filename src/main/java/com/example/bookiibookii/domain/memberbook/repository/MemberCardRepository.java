package com.example.bookiibookii.domain.memberbook.repository;

import com.example.bookiibookii.domain.memberbook.entity.MemberCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
        WHERE mm.user.id = :userId AND mm.group.id = :groupId AND mc.hidden = true
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
        AND c.deletedAt IS NULL
        AND EXISTS (
            SELECT 1 FROM MatchedMember activeMm
            WHERE activeMm.user.id = :userId AND activeMm.group.id = mb.group.id
        )
        ORDER BY mc.updatedAt DESC
        """)
    List<MemberCard> findByUserIdAndBookmarkedTrueWithCardDetailsOrderByCreatedAtDesc(
            @Param("userId") Long userId
    );

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM MemberCard mc WHERE mc.matchedMember.id = :matchedMemberId")
    void deleteByMatchedMember_Id(@Param("matchedMemberId") Long matchedMemberId);

    @Modifying(flushAutomatically = true)
    @Query("""
        DELETE FROM MemberCard mc
        WHERE mc.card.id IN (
            SELECT c.id FROM Cards c
            WHERE c.memberBook.matchedMember.user.id = :userId
        )
        """)
    void deleteAllByCardOwnerUserId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM MemberCard mc WHERE mc.matchedMember.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    /**
     * 서재(MemberBook) 삭제 전 검사: 해당 MemberBook 소속 카드 중
     * 요청자 MatchedMember가 북마크한(삭제되지 않은) 카드가 있는지 여부.
     * 독서카드 삭제({@code BOOKMARKED_CARD_CANNOT_DELETE})와 동일한 북마크 제약.
     */
    @Query("""
        SELECT CASE WHEN COUNT(mc) > 0 THEN true ELSE false END
        FROM MemberCard mc
        JOIN mc.card c
        WHERE c.memberBook.id = :memberBookId
          AND mc.matchedMember.id = :matchedMemberId
          AND mc.bookmarked = true
          AND c.deletedAt IS NULL
        """)
    boolean existsBookmarkedCardByMatchedMemberAndMemberBook(
            @Param("matchedMemberId") Long matchedMemberId,
            @Param("memberBookId") Long memberBookId
    );
}
