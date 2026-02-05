package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.TrackerImage;
import com.example.bookiibookii.domain.tracker.enums.TrackerImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface TrackerImageRepository extends JpaRepository<TrackerImage, Long> {
    Optional<TrackerImage> findByTrackerHistory_IdAndType(Long trackerHistoryId, TrackerImageType type);

    /** historyId 목록 중 해당 type 이미지가 있는 것 중 가장 최근 1건 (N+1 방지용) */
    Optional<TrackerImage> findFirstByTrackerHistory_IdInAndTypeOrderByCreatedAtDesc(
            Collection<Long> trackerHistoryIds, TrackerImageType type);
    boolean existsByS3Key(String s3Key);
    boolean existsByS3KeyAndTrackerHistory_IdNot(String s3Key, Long trackerHistoryId);
    boolean existsByTrackerHistory_IdAndType(Long trackerHistoryId, TrackerImageType type);
}
