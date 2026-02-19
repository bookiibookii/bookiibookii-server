package com.example.bookiibookii.domain.support.inquiry.repository;

import com.example.bookiibookii.domain.support.inquiry.entity.Inquiry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    // [유저용] 내 문의 내역 조회 (기존 유지)
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT i FROM Inquiry i WHERE i.user.id = :userId ORDER BY i.createdAt DESC")
    List<Inquiry> findAllByUserId(@Param("userId") Long userId);

    // [관리자용] 전체 문의 내역 조회 (최신순 + 페치 조인)
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT i FROM Inquiry i ORDER BY i.createdAt DESC")
    Page<Inquiry> findAllOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT i FROM Inquiry i WHERE i.id = :id")
    Optional<Inquiry> findById(@Param("id") Long id);
}
