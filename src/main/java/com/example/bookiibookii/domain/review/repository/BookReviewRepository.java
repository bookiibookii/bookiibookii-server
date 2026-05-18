package com.example.bookiibookii.domain.review.repository;

import com.example.bookiibookii.domain.review.entity.BookReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Long> {

    List<BookReview> findByMemberBook_IdIn(List<Long> memberBookIds);

    boolean existsByMatchedMember_IdAndMemberBook_Id(Long matchedMemberId, Long memberBookId);

    Optional<BookReview> findByMatchedMember_IdAndMemberBook_Id(Long matchedMemberId, Long memberBookId);

    boolean existsByMemberBookId(Long memberBookId);
}
