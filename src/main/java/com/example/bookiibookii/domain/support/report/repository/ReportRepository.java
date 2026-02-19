package com.example.bookiibookii.domain.support.report.repository;

import com.example.bookiibookii.domain.support.report.entity.Report;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report,Long> {
    @EntityGraph(attributePaths = {"user", "group", "group.book"})
    @Query("SELECT r FROM Report r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<Report> findAllByUserId(@Param("userId") Long userId);
}
