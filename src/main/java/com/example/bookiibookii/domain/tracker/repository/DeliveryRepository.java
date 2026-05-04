package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, String> {

    Optional<Delivery> findTopByTrackerOrderByCreatedAtDesc(Tracker tracker);

    Optional<Delivery> findTopByTrackerAndDeliveryStatusOrderByCreatedAtDesc(
            Tracker tracker, DeliveryStatus status);

    @Query("SELECT d FROM Delivery d WHERE d.tracker.group.groupId = :groupId ORDER BY d.createdAt DESC")
    List<Delivery> findAllByGroupIdOrderByCreatedAtDesc(@Param("groupId") Long groupId);

    Optional<Delivery> findTopByTrackerAndReceiverIdAndDeliveryStatusOrderByCreatedAtDesc(
            Tracker tracker, Long receiverId, DeliveryStatus status);

    Optional<Delivery> findTopByTrackerAndSenderIdAndDeliveryStatusOrderByCreatedAtDesc(
            Tracker tracker, Long senderId, DeliveryStatus status);

    boolean existsByTrackerAndDeliveryStatus(Tracker tracker, DeliveryStatus status);

    @Query("SELECT COUNT(d) > 0 FROM Delivery d WHERE d.tracker = :tracker AND d.sender.id = :senderId " +
            "AND d.deliveryStatus = 'SHIPPING'")
    boolean existsShippingBySender(@Param("tracker") Tracker tracker, @Param("senderId") Long senderId);

}
