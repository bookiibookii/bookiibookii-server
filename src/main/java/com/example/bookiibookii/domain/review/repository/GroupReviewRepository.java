package com.example.bookiibookii.domain.review.repository;

import com.example.bookiibookii.domain.review.entity.GroupReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupReviewRepository extends JpaRepository<GroupReview, Long> {

}
