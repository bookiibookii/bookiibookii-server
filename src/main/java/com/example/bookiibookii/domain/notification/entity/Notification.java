package com.example.bookiibookii.domain.notification.entity;

import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "notification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_receiver_dedup_key",
                columnNames = {"receiver_user_id", "dedup_key"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    // 수신자 - notNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiver;

    // 발송자 - 없는 알림도 존재(ex. 키워드알림)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "category", nullable = false, length = 32)
    private NotificationCategory category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 64)
    private NotificationType type; // 라우팅 용도

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message")
    private String message;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload; // JSON string

    @Column(name = "dedup_key", length = 255)
    private String dedupKey;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private Instant readAt;

    // Future scheduledAt/sentAt notification timestamps should also use Instant.
    public void markAsRead(Instant readAt) {
        Objects.requireNonNull(readAt, "readAt must not be null");
        if (!this.read) {
            this.read = true;
            this.readAt = readAt;
        }
    }
}
