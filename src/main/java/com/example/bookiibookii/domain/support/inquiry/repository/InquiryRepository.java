package com.example.bookiibookii.domain.support.inquiry.repository;

import com.example.bookiibookii.domain.support.inquiry.entity.Inquiry;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT i FROM Inquiry i WHERE i.user.id = :userId ORDER BY i.createdAt DESC")
    List<Inquiry> findAllByUserId(@Param("userId") Long userId);
}
