package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.TrackerHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackerHistoryRepository extends JpaRepository<TrackerHistory, Long> {

    // 특정 트래커의 모든 이력을 시간순(최신순)으로 가져올 때 사용.
    List<TrackerHistory> findAllByTrackerIdOrderByCreatedAtDesc(Long trackerId);


}
