package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Application;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    // N+1 문제 해결: 신청서 + 신청자(User)
    @Query("SELECT a DISTINCT FROM Application a " +
            "JOIN FETCH a.guest g " +
            "WHERE a.group.id = :groupId AND a.applicationStatus = 'PENDING'")
    List<Application> findAllWithGuestByGroupId(@Param("groupId") Long groupId);

    // 특정 그룹의 현재 수락된 인원수를 세기 위한 메서드
    long countByGroupIdAndApplicationStatus(Long groupId, ApplicationStatus status);

    //특정 그룹의 '대기 중'인 신청서들만 조회 (Fetch Join으로 Guest 정보까지 한 번에)
    @Query("SELECT a FROM Application a JOIN FETCH a.guest WHERE a.group.id = :groupId AND a.applicationStatus = 'PENDING'")
    List<Application> findAllPendingByGroupId(@Param("groupId") Long groupId);

    // 중복 신청 확인: 특정 그룹에 특정 유저가 이미 신청했는지 여부
    boolean existsByGroupIdAndGuestId(Long groupId, Long guestId);

    //신청 상태값 조회메서드
    boolean existsByGroupIdAndGuestIdAndApplicationStatus(
            Long groupId,
            Long guestId,
            ApplicationStatus status
    );
    //특정 그룹 ID와 게스트(유저) ID로 신청서 엔티티 조회 (참가 목록에서 취소한 사람 제외용도)
    Optional<Application> findByGroupIdAndGuestId(Long groupId, Long guestId);

    // N+1 문제 해결을 위한 배치 쿼리
    // 1번의 쿼리로 여러 그룹의 대기 인원수를 그룹화해서 조회
    @Query("SELECT a.group.id, COUNT(a) FROM Application a " +
            "WHERE a.group.id IN :groupIds AND a.applicationStatus = 'PENDING' " +
            "GROUP BY a.group.id")
    List<Object[]> countPendingByGroupIds(@Param("groupIds") List<Long> groupIds);

    //특정 그룹의 모든 대기 중인 신청서를 일괄 거절 상태로 변경
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Application a SET a.applicationStatus = :status " +
            "WHERE a.group.id = :groupId AND a.applicationStatus = 'PENDING'")
    void updatePendingToRejectedByGroupId(@Param("groupId") Long groupId, @Param("status") ApplicationStatus status);
    @Query("""
    select distinct a.guest.id
    from Application a
    where a.group.id = :groupId
      and a.applicationStatus = :status
    """)
    List<Long> findApplicantUserIdsByGroupIdAndStatus(
            @Param("groupId") Long groupId,
            @Param("status") ApplicationStatus status
    );

    @Query("""
    select distinct a.guest.id
    from Application a
    where a.group.id = :groupId
""")
    List<Long> findApplicantUserIdsByGroupId(@Param("groupId") Long groupId);

    @Query("""
        select a.guest.id
        from Application a
        where a.group.id = :groupId
          and a.applicationStatus = 'PENDING'
    """)
    List<Long> findPendingUserIdsByGroupId(@Param("groupId") Long groupId);

    // 내가 게스트로 '신청 중'인 개수
    long countByGuestIdAndApplicationStatus(Long guestId, ApplicationStatus status);

    // 내가 신청한 그룹 목록 조회 (PENDING/REJECTED, 그룹이 아직 RECRUITING 상태인 것만)
    @Query("""
        SELECT a FROM Application a
        JOIN FETCH a.group g
        JOIN FETCH g.book
        JOIN FETCH g.host h
        LEFT JOIN FETCH h.userImage
        WHERE a.guest.id = :userId
          AND a.applicationStatus IN ('PENDING', 'REJECTED')
          AND g.groupStatus = 'RECRUITING'
        ORDER BY a.createdAt DESC
    """)
    List<Application> findMyActiveApplications(@Param("userId") Long userId);

}

