package com.example.bookiibookii.domain.tracker.entity;

import com.example.bookiibookii.domain.tracker.enums.TrackerImageType;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "tracker_image",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {"tracker_history_id", "type"}
        )
    }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TrackerImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tracker_image_id")
    private Long id;

    @Column(name = "s3_key", length = 255, unique = true, nullable = false)
    private String s3Key;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TrackerImageType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracker_history_id", nullable = false)
    private TrackerHistory trackerHistory;

    // S3 Key 업데이트 메서드
    public void updateS3Key(String newS3Key) {
        this.s3Key = newS3Key;
    }
}
