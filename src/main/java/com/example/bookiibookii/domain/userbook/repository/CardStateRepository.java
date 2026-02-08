package com.example.bookiibookii.domain.userbook.repository;

import com.example.bookiibookii.domain.userbook.entity.CardState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 사용자별 카드 상태(북마크·숨김) 단일 테이블 리포지토리.
 * 북마크 ID 목록, 숨긴 카드 ID 목록, 북마크 토글, 숨김 처리 전부 처리.
 */
@Repository
public interface CardStateRepository extends JpaRepository<CardState, Long> {

    Optional<CardState> findByUser_IdAndCard_Id(Long userId, Long cardId);

    /** 북마크된 카드 ID 목록 (bookmarked = true, cardIds 중에서) */
    @Query("SELECT cs.card.id FROM CardState cs WHERE cs.user.id = :userId AND cs.card.id IN :cardIds AND cs.bookmarked = true")
    Set<Long> findBookmarkedCardIdsByUserIdAndCardIdIn(@Param("userId") Long userId, @Param("cardIds") List<Long> cardIds);

    /** 숨긴 카드 ID 목록 (hidden = true) */
    @Query("SELECT cs.card.id FROM CardState cs WHERE cs.user.id = :userId AND cs.hidden = true")
    List<Long> findHiddenCardIdsByUserId(@Param("userId") Long userId);

    /** 북마크 목록용: bookmarked = true 인 CardState + card·image·book fetch */
    @Query("SELECT cs FROM CardState cs JOIN FETCH cs.card c LEFT JOIN FETCH c.cardImage LEFT JOIN FETCH c.userBook ub LEFT JOIN FETCH ub.group g LEFT JOIN FETCH g.book WHERE cs.user.id = :userId AND cs.bookmarked = true ORDER BY cs.createdAt DESC")
    List<CardState> findByUser_IdAndBookmarkedTrueWithCardAndImageAndBookOrderByCreatedAtDesc(@Param("userId") Long userId);
}
