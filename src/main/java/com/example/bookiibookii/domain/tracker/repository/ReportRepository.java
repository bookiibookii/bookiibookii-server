package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

}