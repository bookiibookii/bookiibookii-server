package com.example.bookiibookii.domain.location.repository;

import com.example.bookiibookii.domain.location.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByAddressAndXAndY(String address, BigDecimal x, BigDecimal y);
}
