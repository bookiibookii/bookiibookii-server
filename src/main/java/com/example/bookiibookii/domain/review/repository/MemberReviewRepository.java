package com.example.bookiibookii.domain.review.repository;

import com.example.bookiibookii.domain.review.entity.MemberReview;
import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberReviewRepository extends JpaRepository<MemberReview, Long> {

    @Query("""
        SELECT COUNT(mr) FROM MemberReview mr
        JOIN mr.target t
        WHERE t.user.id = :userId
          AND mr.reaction = :reaction
    """)
    long countByTargetUserIdAndReaction(@Param("userId") Long userId,
                                        @Param("reaction") MemberReviewReaction reaction);

    boolean existsByGroup_IdAndWriter_Id(Long groupId, Long writerMatchedMemberId);

    @Query("""
        SELECT mr FROM MemberReview mr
        JOIN FETCH mr.writer w
        JOIN FETCH w.user wu
        LEFT JOIN FETCH wu.userImage
        JOIN mr.target t
        WHERE t.user.id = :userId
        ORDER BY mr.createdAt DESC
    """)
    List<MemberReview> findLatestReceivedByUserId(@Param("userId") Long userId, Pageable pageable);
}
