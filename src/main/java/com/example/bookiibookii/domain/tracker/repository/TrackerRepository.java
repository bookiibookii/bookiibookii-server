package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
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
            "join fetch t.bookOwner m " +
            "join fetch m.user " +
            "where t.group.groupId = :groupId")
    Optional<Tracker> findByGroupId(@Param("groupId") Long groupId);


    @Query("SELECT DISTINCT t FROM Tracker t " +
            "JOIN FETCH t.group g " +
            "LEFT JOIN FETCH t.histories " +
            "WHERE g.groupId IN (SELECT mm.group.groupId FROM MatchedMember mm WHERE mm.user.id = :userId) " +
            "ORDER BY t.createdAt DESC")
    List<Tracker> findAllByUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT DISTINCT t FROM Tracker t " +
            "JOIN FETCH t.group g " +
            "JOIN g.matchedMember mm " +
            "LEFT JOIN FETCH t.histories " +
            "WHERE mm.user.id = :userId " +
            "AND mm.role = :role " + // 호스트/게스트 역할 필터 추가
            "ORDER BY t.createdAt DESC")
    List<Tracker> findAllByUserIdAndRoleWithDetails(
            @Param("userId") Long userId,
            @Param("role") RoleStatus role //
    );


    boolean existsByGroup_GroupId(Long aLong);


}
