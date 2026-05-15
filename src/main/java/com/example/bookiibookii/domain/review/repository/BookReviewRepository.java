package com.example.bookiibookii.domain.review.repository;

import com.example.bookiibookii.domain.review.entity.BookReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookReviewRepository extends JpaRepository<BookReview, Long> {

    List<BookReview> findByMemberBook_IdIn(List<Long> memberBookIds);
}
