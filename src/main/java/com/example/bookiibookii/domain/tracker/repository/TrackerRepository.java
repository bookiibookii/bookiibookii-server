package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackerRepository extends JpaRepository<Tracker, Long> {

    @Query("SELECT t FROM Tracker t WHERE t.group.groupId = :groupId")
    Optional<Tracker> findByGroupId(@Param("groupId") Long groupId);

    boolean existsByGroup_GroupId(Long groupId);

    @Query("SELECT t FROM Tracker t JOIN FETCH t.group WHERE t.group.groupId IN :groupIds")
    List<Tracker> findByGroup_GroupIdIn(@Param("groupIds") List<Long> groupIds);

    @Query("SELECT DISTINCT t FROM Tracker t " +
            "JOIN FETCH t.group g " +
            "JOIN MatchedMember mm ON mm.group = g AND mm.user.id = :userId " +
            "JOIN UserBook ub ON ub.group = g AND ub.user.id = :userId " +
            "WHERE ub.rating IS NULL " +
            "ORDER BY t.createdAt DESC")
    List<Tracker> findAllByUserIdWithDetails(@Param("userId") Long userId);

    @Query("SELECT DISTINCT t FROM Tracker t " +
            "JOIN FETCH t.group g " +
            "JOIN MatchedMember mm ON mm.group = g AND mm.user.id = :userId " +
            "JOIN UserBook ub ON ub.group = g AND ub.user.id = :userId " +
            "WHERE mm.role = :role " +
            "AND ub.rating IS NULL " +
            "ORDER BY t.createdAt DESC")
    List<Tracker> findAllByUserIdAndRoleWithDetails(
            @Param("userId") Long userId,
            @Param("role") RoleStatus role
    );
}
