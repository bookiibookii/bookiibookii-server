package com.example.bookiibookii.domain.location.repository;

import com.example.bookiibookii.domain.location.entity.UserDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeliveryRepository extends JpaRepository<UserDelivery, Long> {

    @Query("SELECT ud FROM UserDelivery ud JOIN FETCH ud.location WHERE ud.user.id = :userId ORDER BY ud.createdAt ASC")
    List<UserDelivery> findByUserIdWithLocation(@Param("userId") Long userId);

    long countByUser_Id(Long userId);

    Optional<UserDelivery> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByIdAndUser_Id(Long id, Long userId);

    Optional<UserDelivery> findFirstByUser_IdOrderByCreatedAtAsc(Long userId);

    Optional<UserDelivery> findByUser_IdAndIsDefaultTrue(Long userId);

    Optional<UserDelivery> findFirstByUser_IdAndIdNotOrderByCreatedAtAsc(Long userId, Long excludeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE user_delivery SET is_default = (user_delivery_id = :targetId) WHERE user_id = :userId", nativeQuery = true)
    void updateDefaultDelivery(@Param("userId") Long userId, @Param("targetId") Long targetId);
}
