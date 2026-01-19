package com.example.bookiibookii.domain.group.repository;



import com.example.bookiibookii.domain.group.entity.MatchedMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchedMemberRepository extends JpaRepository<MatchedMember, Long> {

    @Query("SELECT mm FROM MatchedMember mm " +
            "JOIN FETCH mm.group g " +
            "JOIN FETCH g.tracker t " +  // g.getTracker()를 탐색하는 방식
            "WHERE mm.userId.id = :userId")
    List<MatchedMember> findAllByUserIdWithTracker(@Param("userId") Long userId);


    Optional<MatchedMember> findByGroup_GroupIdAndReadingOrder(Long groupId, int readingOrder);

}