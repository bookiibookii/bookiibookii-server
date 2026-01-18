package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.Tracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface TrackerRepository extends JpaRepository<Tracker, Long> {

    // 매칭 ID로 트래커를 찾아야 할 때 사용.
    // Optional<Tracker> findByMatchedGroupId(Long matchedGroupId);


    @Query("select t from Tracker t " +
            "join fetch t.currentMember m " +
            "join fetch m.userId " +
            "where t.groupId = :groupId")
    Optional<Tracker> findByGroupId(@Param("groupId") Long groupId);

//    @Query("SELECT t FROM Tracker t " +
//            "JOIN FETCH t.groupId g " +
//            "WHERE g IN (SELECT mm.group FROM MatchedMember mm WHERE mm.userId.id = :userId)")
//    List<Tracker> findAllByUserIdWithGroup(@Param("userId") Long userId);


}
