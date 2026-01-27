package com.example.bookiibookii.domain.group.repository;



import com.example.bookiibookii.domain.group.entity.Groups;
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
            "WHERE mm.user.id = :userId")
    List<MatchedMember> findAllByUserIdWithTracker(@Param("userId") Long userId);

@Query("SELECT mm FROM MatchedMember mm " +
        "JOIN FETCH mm.group g " +
        "WHERE g.groupId = :groupId AND mm.readingOrder = :readingOrder")
Optional<MatchedMember> findByGroupAndOrder(@Param("groupId") Long groupId, @Param("readingOrder") int readingOrder);

    //현재까지의 참여맴버 수
    long countByGroup(Groups groups);

    //참여맴버 리스트(읽는 순서대로 정렬)
    List<MatchedMember> findAllByGroupOrderByReadingOrderAsc(Groups group);
}