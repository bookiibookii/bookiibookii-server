package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupsRepository extends JpaRepository<Groups, Long> {
    // 전체 모집 중인 그룹 조회 (Slice 적용)
    @Query("select g from Groups g " +
            "join fetch g.book " +
            "join fetch g.host " +
            "where g.groupStatus = 'RECRUITING'")
    Slice<Groups> findAllWithBookAndHost(Pageable pageable);

    // 지역별 필터링 조회 (Slice 적용) 그룹리스트api용?
//    @Query("select g from Groups g " +
//            "join fetch g.book " +
//            "join fetch g.host h " +
//            "where h.region = :region " +
//            "and g.tradeType = 'DIRECT' " +
//            "and g.groupStatus != 'DELETED'")
//    Slice<Groups> findAllByHostRegion(@Param("region") String region, Pageable pageable);

    // [추가] 동시성 제어를 위한 비관적 락 메서드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Groups g where g.id = :groupId and g.groupStatus != 'DELETED'")
    Optional<Groups> findByIdForUpdate(@Param("groupId") Long groupId);

    // 신청 시 동시성 제어용 — 락 + book/host fetch join
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    select g from Groups g
    join fetch g.book
    join fetch g.host
    where g.id = :groupId
    and g.groupStatus != 'DELETED'
    """)
    Optional<Groups> findByIdForUpdateWithBookAndHost(@Param("groupId") Long groupId);


    // 랜덤 그룹 조회 (이미 뽑힌 그룹 제외)
    @Query("""
      select g
      from Groups g
      join fetch g.book
      where g.id not in :excludedIds
        and g.groupStatus = :status
        and g.host.id != :userId
      order by function('rand')
    """)
    List<Groups> findRandomGroupsExcludingFetchBook(
            @Param("userId") Long userId,
            @Param("excludedIds") List<Long> excludedIds,
            @Param("status") GroupStatus status,
            Pageable pageable
    );
  
    //그룹 상세 조회 (host, host.userImage fetch join으로 hostProfileImageUrl N+1 방지)
    @Query("SELECT g FROM Groups g " +
            "JOIN FETCH g.book " +
            "JOIN FETCH g.host h " +
            "LEFT JOIN FETCH h.userImage " +
            "WHERE g.id = :groupId AND g.groupStatus != 'DELETED'")
    Optional<Groups> findDetailById(@Param("groupId") Long groupId);

    // fetch join 적용된 기본 그룹 조회
    @Query("""
    select g from Groups g
    join fetch g.book
    join fetch g.host
    where g.id = :groupId
    and g.groupStatus != 'DELETED'
    """)
    Optional<Groups> findByIdWithBookAndHost(@Param("groupId") Long groupId);

    // 내가 방장인 그룹 중 '모집 중' 또는 '진행 중'인 개수
    long countByHostIdAndGroupStatusIn(Long hostId, List<GroupStatus> statuses);

    @Query("""
    select g from Groups g
    join fetch g.book
    join fetch g.host h
    left join fetch h.userImage
    where h.id = :hostId
      and g.groupStatus <> :deletedStatus
    order by g.createdAt desc, g.id desc
    """)
    List<Groups> findMyHostedGroups(
            @Param("hostId") Long hostId,
            @Param("deletedStatus") GroupStatus deletedStatus
    );

    // PARTNER_REVIEWING 진입 후 14일 이상 파트너 후기 미작성 그룹 조회
    @Query(value = """
            SELECT g.* FROM `groups` g
            WHERE g.group_status = 'MATCHED'
            AND (
                SELECT COUNT(*) FROM matchedmember mm
                WHERE mm.group_id = g.group_id
                AND mm.reading_status = 'PARTNER_REVIEWING'
                AND mm.partner_reviewing_started_at <= :cutoff
            ) >= 2
            """, nativeQuery = true)
    List<Groups> findGroupsForForceComplete(@Param("cutoff") LocalDateTime cutoff);
}
