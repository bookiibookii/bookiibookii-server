package com.example.bookiibookii.domain.userbook.entity;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 그룹 내 공유 카드를 "내 화면에서만 숨김" 처리한 기록.
 * 카드/그룹은 삭제되지 않고, 해당 사용자만 목록·상세에서 제외된다.
 */
@Entity
@Table(
        name = "deleted_card",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "card_id"})
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeletedCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deleted_card_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;
}
