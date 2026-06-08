package com.example.bookiibookii.domain.review.repository;

import com.example.bookiibookii.domain.review.entity.BookReview;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Long> {

    List<BookReview> findByMemberBook_IdIn(List<Long> memberBookIds);

    boolean existsByMatchedMember_IdAndMemberBook_Id(Long matchedMemberId, Long memberBookId);

    Optional<BookReview> findByMatchedMember_IdAndMemberBook_Id(Long matchedMemberId, Long memberBookId);

    @Query("""
        SELECT br FROM BookReview br
        JOIN FETCH br.memberBook mb
        JOIN FETCH mb.book
        JOIN FETCH br.matchedMember mm
        WHERE mm.id = :matchedMemberId
        AND mm.group.id = :groupId
        ORDER BY mb.isMine DESC, br.createdAt ASC
        """)
    List<BookReview> findMyBookReviewsWithBook(
            @Param("matchedMemberId") Long matchedMemberId,
            @Param("groupId") Long groupId
    );

    Optional<BookReview> findByIdAndMatchedMember_IdAndMatchedMember_Group_Id(
            Long reviewId,
            Long matchedMemberId,
            Long groupId
    );

    boolean existsByMemberBookId(Long memberBookId);

    @Query("""
        SELECT br FROM BookReview br
        JOIN FETCH br.memberBook mb
        JOIN FETCH mb.book
        JOIN FETCH mb.group g
        JOIN FETCH br.matchedMember mm
        WHERE mm.user.id = :userId
        AND mb.removedAt IS NULL
        ORDER BY br.updatedAt DESC
        """)
    List<BookReview> findReviewedBooksByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT COUNT(br) FROM BookReview br
        JOIN br.matchedMember mm
        JOIN br.memberBook mb
        WHERE mm.user.id = :userId
        AND mb.removedAt IS NULL
        """)
    long countReviewedBooksByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT CASE WHEN COUNT(br) > 0 THEN true ELSE false END
        FROM BookReview br
        JOIN br.matchedMember mm
        JOIN br.memberBook mb
        WHERE mm.user.id = :userId
        AND mb.book.id = :bookId
        AND mb.removedAt IS NULL
        """)
    boolean existsReviewedBookByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

    @Query("""
        SELECT br FROM BookReview br
        JOIN FETCH br.memberBook mb
        JOIN FETCH mb.book
        JOIN FETCH br.matchedMember mm
        JOIN FETCH mm.user u
        LEFT JOIN FETCH u.userImage
        WHERE mm.group.id = :groupId
        ORDER BY br.createdAt ASC
        """)
    List<BookReview> findAllByGroupIdWithDetails(@Param("groupId") Long groupId);

    @Query("""
        SELECT br FROM BookReview br
        JOIN FETCH br.memberBook mb
        JOIN FETCH mb.book
        JOIN FETCH br.matchedMember mm
        JOIN FETCH mm.user u
        LEFT JOIN FETCH u.userImage
        WHERE mm.id = :matchedMemberId
        AND mm.group.id = :groupId
        ORDER BY br.createdAt ASC
        """)
    List<BookReview> findAllByMatchedMemberIdAndGroupIdWithDetails(
            @Param("matchedMemberId") Long matchedMemberId,
            @Param("groupId") Long groupId
    );
}
