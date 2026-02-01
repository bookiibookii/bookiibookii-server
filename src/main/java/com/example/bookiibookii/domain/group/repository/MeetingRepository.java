package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    // 그룹 ID와 트래커 상태를 조합하여 정확한 시점의 약속을 조회
    Optional<Meeting> findByGroup_GroupIdAndTrackerStatus(Long groupId, TrackerStatus status);
}