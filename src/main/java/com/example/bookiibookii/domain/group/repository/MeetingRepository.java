package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    // 그룹 ID로 약속 정보를 찾는 메서드
    Optional<Meeting> findByGroupGroupId(Long groupId);

}
