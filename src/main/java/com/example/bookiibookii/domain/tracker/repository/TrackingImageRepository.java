package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.TrackingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrackingImageRepository extends JpaRepository<TrackingImage, Long> {

    Optional<TrackingImage> findTopByDelivery_IdOrderByCreatedAtDesc(String deliveryId);

    Optional<TrackingImage> findTopByDelivery_IdOrderByCreatedAtAsc(String deliveryId);

    boolean existsByS3Key(String s3Key);
}
