package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.user.entity.ProfileShareToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileShareTokenRepository extends JpaRepository<ProfileShareToken, Long> {

    @Query("""
        SELECT t FROM ProfileShareToken t
        WHERE t.user.id = :userId
        AND t.revokedAt IS NULL
        """)
    List<ProfileShareToken> findAllActiveByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT t FROM ProfileShareToken t
        JOIN FETCH t.user u
        LEFT JOIN FETCH u.userImage
        WHERE t.token = :token
        AND t.revokedAt IS NULL
        """)
    Optional<ProfileShareToken> findActiveByTokenWithUserDetails(@Param("token") String token);
}
