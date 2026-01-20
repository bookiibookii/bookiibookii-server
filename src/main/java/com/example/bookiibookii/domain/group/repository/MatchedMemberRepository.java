package com.example.bookiibookii.domain.group.repository;



import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchedMemberRepository extends JpaRepository<MatchedMember, Long> {

    @Query("SELECT mm FROM MatchedMember mm " +
            "JOIN FETCH mm.group g " +
            "JOIN FETCH g.tracker t " +  // g.getTracker()를 탐색하는 방식
            "WHERE mm.userId.id = :userId")
    List<MatchedMember> findAllByUserIdWithTracker(@Param("userId") Long userId);

    //특정 그룹에 현재 확정된 멤버 (방장포함) 몇명인지 계산
    long countByGroup(Groups groups);
}
