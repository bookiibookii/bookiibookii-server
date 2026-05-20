package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.user.entity.UserWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWithdrawalRepository extends JpaRepository<UserWithdrawal, Long> {
}
