package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
}
