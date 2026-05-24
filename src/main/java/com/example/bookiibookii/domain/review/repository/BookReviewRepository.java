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

    boolean existsByMemberBookId(Long memberBookId);

  /** GroupBook.findReviewedBooksByUserId 대체 */
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

  /** GroupBook.countReviewedBooksByUserId 대체 */
  @Query("""
      SELECT COUNT(br) FROM BookReview br
      JOIN br.matchedMember mm
      JOIN br.memberBook mb
      WHERE mm.user.id = :userId
      AND mb.removedAt IS NULL
      """)
  long countReviewedBooksByUserId(@Param("userId") Long userId);

  /** GroupBook.existsByUser_IdAndBook_IdAndRatingIsNotNull 대체 */
  @Query("""
      SELECT CASE WHEN COUNT(br) > 0 THEN true ELSE false END
      FROM BookReview br
      JOIN br.matchedMember mm
      JOIN br.memberBook mb
      WHERE mm.user.id = :userId
      AND mb.book.id = :bookId
      """)
  boolean existsReviewedBookByUserIdAndBookId(@Param("userId") Long userId, @Param("bookId") Long bookId);

  /** GroupBook.findByGroup_IdInWithUserAndGroup(책 리뷰) 대체 */
  @Query("""
      SELECT br FROM BookReview br
      JOIN FETCH br.memberBook mb
      JOIN FETCH mb.matchedMember mm
      JOIN FETCH mm.user
      JOIN FETCH mb.group g
      WHERE g.id IN :groupIds
      """)
  List<BookReview> findByGroup_IdInWithMemberBookAndUser(@Param("groupIds") List<Long> groupIds);
}

