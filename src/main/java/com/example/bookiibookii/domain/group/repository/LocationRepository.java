package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, String> {
}
