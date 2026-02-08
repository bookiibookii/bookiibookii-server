package com.example.bookiibookii.domain.notification.repository;

import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        select n from Notification n
        where n.receiver.id = :receiverId
          and n.category = :category
        order by n.read asc, n.createdAt desc, n.id desc
    """)
    List<Notification> findFirstPage(
            @Param("receiverId") Long receiverId,
            @Param("category") NotificationCategory category,
            Pageable pageable
    );

    @Query("""
        select n from Notification n
        where n.receiver.id = :receiverId
          and n.category = :category
          and (
               n.read > :cursorRead
            or (n.read = :cursorRead and n.createdAt < :cursorCreatedAt)
            or (n.read = :cursorRead and n.createdAt = :cursorCreatedAt and n.id < :cursorId)
          )
        order by n.read asc, n.createdAt desc, n.id desc
    """)
    List<Notification> findNextPage(
            @Param("receiverId") Long receiverId,
            @Param("category") NotificationCategory category,
            @Param("cursorRead") boolean cursorRead,
            @Param("cursorCreatedAt") Instant cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}