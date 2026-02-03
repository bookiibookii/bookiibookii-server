package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    // 그룹 ID와 트래커 상태를 조합하여 정확한 시점의 약속을 조회
    @Query("SELECT m FROM Meeting m WHERE m.group.groupId = :groupId AND m.trackerStatus = :status")
    Optional<Meeting> findByGroup_GroupIdAndTrackerStatus(Long groupId, TrackerStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Meeting m WHERE m.group.groupId = :groupId AND m.trackerStatus = :status")
    Optional<Meeting> findByGroupWithLock(@Param("groupId") Long groupId, @Param("status") TrackerStatus status);
}