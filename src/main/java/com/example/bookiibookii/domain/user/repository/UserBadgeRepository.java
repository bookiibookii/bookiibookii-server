package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    // 유저가 획득 배지 리스트 조회
    List<UserBadge> findByUserAndCountGreaterThan(User user, Integer count);
}
