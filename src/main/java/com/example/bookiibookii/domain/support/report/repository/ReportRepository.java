package com.example.bookiibookii.domain.support.report.repository;

import com.example.bookiibookii.domain.support.report.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report,Long> {
    // since가 null이 아니면 재가입 이후 항목만 반환
    @EntityGraph(attributePaths = {"user", "group", "group.book"})
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId AND (:since IS NULL OR r.createdAt >= :since) ORDER BY r.createdAt DESC")
    List<Report> findAllByUserId(@Param("userId") Long userId, @Param("since") Instant since);

    // [관리자용] 전체 신고 내역 조회 (최신순 + 페치 조인)
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT i FROM Report i ORDER BY i.createdAt DESC")
    Page<Report> findAllOrderByCreatedAtDesc(Pageable pageable);
}
