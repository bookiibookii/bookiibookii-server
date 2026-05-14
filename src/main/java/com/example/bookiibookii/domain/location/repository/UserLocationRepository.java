package com.example.bookiibookii.domain.location.repository;

import com.example.bookiibookii.domain.location.entity.UserLocation;
import com.example.bookiibookii.domain.location.enums.LocationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLocationRepository extends JpaRepository<UserLocation, Long> {

    @Query("SELECT ul FROM UserLocation ul JOIN FETCH ul.location WHERE ul.user.id = :userId ORDER BY ul.createdAt ASC")
    List<UserLocation> findByUserIdWithLocation(@Param("userId") Long userId);

    long countByUser_IdAndType(Long userId, LocationType type);

    Optional<UserLocation> findByUserLocationIdAndUser_Id(Long userLocationId, Long userId);
}
