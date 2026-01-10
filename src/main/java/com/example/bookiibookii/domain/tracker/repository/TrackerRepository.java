package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.Tracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface TrackerRepository extends JpaRepository<Tracker, Long> {

    // 매칭 ID로 트래커를 찾아야 할 때 사용.
    // Optional<Tracker> findByMatchedGroupId(Long matchedGroupId);

}
