package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);
    boolean existsByName(String name);

    @Query("""
    SELECT u FROM User u
    WHERE u.id = :id
    """)
    Optional<User> findByIdIncludingWithdrawn(@Param("id") Long id);

    // status='ACTIVE' 필터 무시용 (WITHDRAWN까지 조회하여 재가입 로직 구성)
    @Query("""
    SELECT u FROM User u
    WHERE u.socialId = :socialId
      AND u.socialType = :socialType
    """)
    Optional<User> findBySocialIdAndSocialType(
            @Param("socialId") String socialId,
            @Param("socialType") SocialType socialType
    );

    @Modifying
    @Query("""
    DELETE FROM User u
    WHERE u.status = 'WITHDRAWN'
    AND u.updatedAt <= :deleteBefore
    """)
    int deleteWithdrawnUsersBefore(@Param("deleteBefore") LocalDateTime deleteBefore);

}
