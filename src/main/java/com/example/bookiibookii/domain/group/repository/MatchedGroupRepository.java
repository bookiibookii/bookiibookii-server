package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.MatchedGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchedGroupRepository extends JpaRepository<MatchedGroup, Long> {
}
