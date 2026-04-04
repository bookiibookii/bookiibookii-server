package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.user.entity.UserPick;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPickRepository extends JpaRepository<UserPick, Long> {
}
