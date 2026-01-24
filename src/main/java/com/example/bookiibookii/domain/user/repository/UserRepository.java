package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.tag.enums.TagType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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


    // 태그 기반 매칭 후보 조회
    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN FETCH u.userTags ut " +
            "JOIN FETCH ut.tag t " +
            "WHERE u.id <> :userId " +
            "AND t.type IN :targetTypes " +
            "AND EXISTS (" +
            "  SELECT 1 FROM Groups g " +
            "  WHERE g.host.id = u.id " +
            "  AND g.groupStatus = :status)")
    List<User> findHostsWithTargetTags(
            @Param("userId") Long userId,
            @Param("targetTypes") List<TagType> targetTypes,
            @Param("status") GroupStatus status
    );

    // targetTypes 태그가 없는 '모집 중'그룹의 호스트 유저 조회
    @Query("SELECT u FROM User u " +
            "WHERE u.id <> :userId " +
            "AND NOT EXISTS (" +  // 해당 타입의 태그가 존재하지 않는 사람 찾기
            "   SELECT 1 FROM UserTag ut " +
            "   JOIN ut.tag t " +
            "   WHERE ut.user = u " +
            "   AND t.type IN :targetTypes) " +
            "AND EXISTS (" +
            "  SELECT 1 FROM Groups g " +
            "  WHERE g.host.id = u.id " +
            "  AND g.groupStatus = :status)")
    List<User> findHostsWithoutTargetTags(
            @Param("userId") Long userId,
            @Param("targetTypes") List<TagType> targetTypes,
            @Param("status") GroupStatus status
    );

    // 랜덤 유저 1명 조회 ('모집 중'그룹의 호스트 유저)
    @Query(value = "SELECT * FROM users u " +
            "WHERE u.id <> :userId " +
            "AND EXISTS (" +
            "  SELECT 1 FROM groups g " +
            "  WHERE g.host_id = u.id " +
            "  AND g.group_status = :status) " +
            "ORDER BY RAND() " +
            "LIMIT 1", nativeQuery = true)
    Optional<User> findOneRandomHost(@Param("userId") Long userId, @Param("status") String status);
}
