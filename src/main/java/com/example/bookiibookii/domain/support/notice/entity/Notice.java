package com.example.bookiibookii.domain.support.notice.entity;

import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notice")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // 작성자
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "summary", length = 255, nullable = false)
    private String summary;

    @Column(name = "content", length = 2000, nullable = false)
    private String content;

    @Column(name = "image")
    private String image;
}
