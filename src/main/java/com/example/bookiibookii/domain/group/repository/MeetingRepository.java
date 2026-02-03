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

// 그룹 ID를 기반으로 가장 최근에 생성된(Id가 큰) 약속 1건을 조회
    @Query(value = "SELECT * FROM meeting WHERE group_id = :groupId ORDER BY meeting_id DESC LIMIT 1", nativeQuery = true)
    Optional<Meeting> findLatestByGroupIdNative(@Param("groupId") Long groupId);

    @Query(value = "SELECT * FROM meeting WHERE group_id = :groupId AND tracker_status = :status LIMIT 1", nativeQuery = true)
    Optional<Meeting> findByGroupIdAndStatusNative(@Param("groupId") Long groupId, @Param("status") String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Meeting m WHERE m.group.groupId = :groupId AND m.trackerStatus = :status")
    Optional<Meeting> findByGroupWithLock(@Param("groupId") Long groupId, @Param("status") TrackerStatus status);
}