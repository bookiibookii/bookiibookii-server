package com.example.bookiibookii.domain.review.repository;

import com.example.bookiibookii.domain.review.entity.MemberReview;
import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberReviewRepository extends JpaRepository<MemberReview, Long> {

    @Query("""
        SELECT COUNT(mr) FROM MemberReview mr
        JOIN mr.target t
        WHERE t.user.id = :userId
          AND mr.reaction = :reaction
          AND (:since IS NULL OR mr.createdAt > :since)
    """)
    long countByTargetUserIdAndReaction(@Param("userId") Long userId,
                                        @Param("reaction") MemberReviewReaction reaction,
                                        @Param("since") Instant since);

    boolean existsByGroup_IdAndWriter_Id(Long groupId, Long writerMatchedMemberId);

    @Query(
            value = """
                SELECT mr FROM MemberReview mr
                JOIN FETCH mr.writer w
                JOIN FETCH w.user wu
                LEFT JOIN FETCH wu.userImage
                JOIN mr.target t
                WHERE t.user.id = :userId
                AND (:since IS NULL OR mr.createdAt > :since)
                ORDER BY mr.createdAt DESC, mr.id DESC
                """,
            countQuery = """
                SELECT COUNT(mr) FROM MemberReview mr
                JOIN mr.target t
                WHERE t.user.id = :userId
                AND (:since IS NULL OR mr.createdAt > :since)
                """
    )
    Page<MemberReview> findReceivedReviewsByUserId(
            @Param("userId") Long userId,
            @Param("since") Instant since,
            Pageable pageable
    );

    @Query("""
        SELECT mr FROM MemberReview mr
        JOIN FETCH mr.writer w
        JOIN FETCH w.user wu
        LEFT JOIN FETCH wu.userImage
        JOIN mr.target t
        WHERE t.user.id = :userId
        AND (:since IS NULL OR mr.createdAt > :since)
        ORDER BY mr.createdAt DESC
    """)
    List<MemberReview> findLatestReceivedByUserId(@Param("userId") Long userId, @Param("since") Instant since, Pageable pageable);

    @Query("""
        SELECT mr FROM MemberReview mr
        JOIN FETCH mr.group g
        JOIN FETCH mr.writer w
        JOIN FETCH w.user wu
        LEFT JOIN FETCH wu.userImage
        JOIN FETCH mr.target t
        JOIN FETCH t.user tu
        WHERE g.id = :groupId
        ORDER BY mr.createdAt ASC
        """)
    List<MemberReview> findAllByGroupIdWithDetails(@Param("groupId") Long groupId);

    @Query("""
        SELECT mr FROM MemberReview mr
        JOIN FETCH mr.group g
        JOIN FETCH mr.writer w
        JOIN FETCH w.user wu
        LEFT JOIN FETCH wu.userImage
        WHERE g.id = :groupId
        AND w.id = :writerMatchedMemberId
        """)
    Optional<MemberReview> findByGroupIdAndWriterIdWithDetails(
            @Param("groupId") Long groupId,
            @Param("writerMatchedMemberId") Long writerMatchedMemberId
    );
}
