package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
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

    @Query(value = "SELECT * FROM meeting WHERE tracker_id = :trackerId AND tracker_status = :status LIMIT 1", nativeQuery = true)
    Optional<Meeting> findByTrackerIdAndStatusNative(@Param("trackerId") Long trackerId, @Param("status") String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Meeting m WHERE m.tracker.id = :trackerId AND m.trackerStatus = :status")
    Optional<Meeting> findByTrackerWithLock(@Param("trackerId") Long trackerId, @Param("status") TrackerStatus status);

    Optional<Meeting> findFirstByTrackerOrderByCreatedAtDesc(Tracker tracker);
}
