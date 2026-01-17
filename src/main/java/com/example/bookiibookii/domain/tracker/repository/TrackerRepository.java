package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.Tracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface TrackerRepository extends JpaRepository<Tracker, Long> {

    // 매칭 ID로 트래커를 찾아야 할 때 사용.
    // Optional<Tracker> findByMatchedGroupId(Long matchedGroupId);

    @Query("select t from Tracker t join fetch t.currentMember m join fetch m.userId where t.id = :trackerId")
    Optional<Tracker> findDetailById(@Param("trackerId") Long trackerId);

}
