package com.example.bookiibookii.domain.group.repository;



import com.example.bookiibookii.domain.group.entity.MatchedMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MatchedMemberRepository extends JpaRepository<MatchedMember, Long> {

    @Query("SELECT mm FROM MatchedMember mm " +
            "JOIN FETCH mm.group g " +
            "JOIN FETCH Tracker t ON t.group = g " +
            "WHERE mm.userId.id = :userId")
    List<MatchedMember> findAllByUserIdWithTracker(@Param("userId") Long userId);


}