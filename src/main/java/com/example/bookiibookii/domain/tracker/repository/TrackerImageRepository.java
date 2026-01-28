package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.TrackerImage;
import com.example.bookiibookii.domain.tracker.enums.TrackerImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrackerImageRepository extends JpaRepository<TrackerImage, Long> {
    Optional<TrackerImage> findByTrackerHistory_IdAndType(Long trackerHistoryId, TrackerImageType type);
    boolean existsByS3Key(String s3Key);
    boolean existsByS3KeyAndTrackerHistory_IdNot(String s3Key, Long trackerHistoryId);
    boolean existsByTrackerHistory_IdAndType(Long trackerHistoryId, TrackerImageType type);
}
