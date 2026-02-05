package com.example.bookiibookii.domain.review.repository;

import com.example.bookiibookii.domain.review.entity.GroupReviewBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupReviewBadgeRepository extends JpaRepository<GroupReviewBadge, Long> {
}
