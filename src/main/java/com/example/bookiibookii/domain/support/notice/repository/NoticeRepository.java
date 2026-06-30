package com.example.bookiibookii.domain.support.notice.repository;

import com.example.bookiibookii.domain.support.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByUpdatedAtDesc();
}
