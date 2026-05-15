package com.example.bookiibookii.domain.location.repository;

import com.example.bookiibookii.domain.location.entity.UserExchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserExchangeRepository extends JpaRepository<UserExchange, Long> {

    // 추천 섹션(REGION_EXCHANGE)에서도 사용
    @Query("SELECT ue FROM UserExchange ue JOIN FETCH ue.location WHERE ue.user.id = :userId ORDER BY ue.createdAt ASC")
    List<UserExchange> findByUserIdWithLocation(@Param("userId") Long userId);

    long countByUser_Id(Long userId);

    Optional<UserExchange> findByUserExchangeIdAndUser_Id(Long Id, Long userId);
}
