package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.Role;
import com.example.bookiibookii.domain.user.enums.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);
    Optional<User> findByNickName(String name);
    boolean existsByNickName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") Long userId);

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

    // 탈퇴 처리: entity dirty checking 에 의존하지 않고 직접 UPDATE
    // (OSIV 환경에서 JwtAuthFilter가 read-only로 로드한 엔티티는 snapshot 없음 → dirty check 미동작)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE User u SET u.status = 'WITHDRAWN', u.updatedAt = CURRENT_TIMESTAMP WHERE u.id = :userId")
    void withdrawUser(@Param("userId") Long userId);

    // revoke 재시도 대상: WITHDRAWN + APPLE + appleRefreshToken 있는 유저
    @Query("""
    SELECT u FROM User u
    WHERE u.status = 'WITHDRAWN'
    AND u.updatedAt <= :deleteBefore
    AND u.socialType = 'APPLE'
    AND u.appleRefreshToken IS NOT NULL
    """)
    List<User> findWithdrawnAppleUsersForRevoke(@Param("deleteBefore") Instant deleteBefore);

    // revoke 성공 후 토큰 제거 (다음 삭제 대상에 포함되도록)
    // 토큰값·WITHDRAWN 상태 일치 조건: 조회 후 재가입한 유저의 새 토큰을 덮어쓰지 않도록 방어
    @Modifying
    @Query("""
    UPDATE User u SET u.appleRefreshToken = null
    WHERE u.id = :userId
      AND u.appleRefreshToken = :expectedToken
      AND u.status = 'WITHDRAWN'
    """)
    int clearAppleRefreshToken(
            @Param("userId") Long userId,
            @Param("expectedToken") String expectedToken);

    // appleRefreshToken이 남아있는 APPLE 유저는 revoke 미완료이므로 삭제 제외
    @Modifying
    @Query("""
    DELETE FROM User u
    WHERE u.status = 'WITHDRAWN'
    AND u.updatedAt <= :deleteBefore
    AND (u.socialType != 'APPLE' OR u.appleRefreshToken IS NULL)
    """)
    int deleteWithdrawnUsersBefore(@Param("deleteBefore") Instant deleteBefore);

    /*
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
    */

    // 랜덤 유저 1명 조회 ('모집 중'그룹의 호스트 유저)
    @Query(value = "SELECT * FROM users u " +
            "WHERE u.id <> :userId " +
            "AND EXISTS (" +
            "  SELECT 1 FROM `groups` g " +
            "  WHERE g.host_id = u.id " +
            "  AND g.group_status = :status) " +
            "ORDER BY RAND() " +
            "LIMIT 1", nativeQuery = true)
    Optional<User> findOneRandomHost(@Param("userId") Long userId, @Param("status") String status);

    @Query("select u.nickName from User u where u.id = :userId")
    Optional<String> findNickNameById(@Param("userId") Long userId);

    List<User> findAllByRoleOrderByIdAsc(Role role);
}
