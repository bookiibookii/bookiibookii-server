package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchedGroupRepository extends JpaRepository<MatchedMember, Long> {
}
