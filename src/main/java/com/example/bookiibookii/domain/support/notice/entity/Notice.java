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

    // 최종 수정자 (최초 등록 후 수정된 적 없으면 null)
    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "summary", length = 255, nullable = false)
    private String summary;

    @Column(name = "content", length = 2000, nullable = false)
    private String content;

    public void updateTitle(String title) {
        this.title = title;
    }
    public void updateContent(String content) {
        this.content = content;
    }
    public void updateSummary(String summary) {
        this.summary = summary;
    }
    public void updateUpdatedBy(Long userId) {
        this.updatedByUserId = userId;
    }
}
