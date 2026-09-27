package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.book.entity.Book;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

// 홈 인기 도서 집계 결과 (groups에서 파생된 읽기용 데이터, PopularBookScheduler가 주기적으로 전체 교체)
@Entity
@Table(
        name = "popular_book",
        uniqueConstraints = @UniqueConstraint(name = "uk_popular_book_book", columnNames = "book_id"),
        indexes = @Index(name = "idx_popular_book_ranking", columnList = "ranking")
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PopularBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "popular_book_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    // 삭제되지 않은 그룹 수 (인기 점수)
    @Column(name = "group_count", nullable = false)
    private Long groupCount;

    // 동점일 때 최근 생성 그룹 순 정렬용
    @Column(name = "last_group_created_at", nullable = false)
    private Instant lastGroupCreatedAt;

    @Column(name = "ranking", nullable = false)
    private Integer ranking;

    @Column(name = "aggregated_at", nullable = false)
    private Instant aggregatedAt;
}
