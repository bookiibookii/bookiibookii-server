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
    @Query("select g from Groups g " +
            "join fetch g.book " +
            "join fetch g.host h " +
            "where h.region = :region " +
            "and g.tradeType = 'DIRECT' " +
            "and g.groupStatus != 'DELETED'")
    Slice<Groups> findAllByHostRegion(@Param("region") String region, Pageable pageable);

    // [추가] 동시성 제어를 위한 비관적 락 메서드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Groups g where g.groupId = :groupId and g.groupStatus != 'DELETED'")
    Optional<Groups> findByIdForUpdate(@Param("groupId") Long groupId);


    // UserTag와 GroupTag 일치도가 높은 그룹 조회
    @Query("SELECT g FROM Groups g " +
            "JOIN g.groupTags gt " +
            "WHERE gt.tag.id IN :tagIds " +
            "AND g.groupStatus = :status " +
            "GROUP BY g " +
            "ORDER BY COUNT(gt) DESC")
    List<Groups> findGroupsByTagMatching(
            @Param("tagIds") List<Long> tagIds,
            @Param("status") GroupStatus status,
            Pageable pageable
    );

    // 랜덤 그룹 조회 (이미 뽑힌 그룹 제외)
    @Query(value = "SELECT * FROM `groups` g " +
            "WHERE g.group_id NOT IN :excludedIds " +
            "AND g.group_status = :status " +
            "ORDER BY RAND() " +
            "LIMIT :limit", nativeQuery = true)
    List<Groups> findRandomGroupsExcluding(
            @Param("excludedIds") List<Long> excludedIds,
            @Param("status") String status,
            @Param("limit") int limit
    );
  
    //그룹 상세 조회 (host, host.userImage fetch join으로 hostProfileImage N+1 방지)
    @Query("SELECT g FROM Groups g " +
            "JOIN FETCH g.book " +
            "JOIN FETCH g.host h " +
            "LEFT JOIN FETCH h.userImage " +
            "WHERE g.groupId = :groupId AND g.groupStatus != 'DELETED'")
    Optional<Groups> findDetailById(@Param("groupId") Long groupId);

    // fetch join 적용된 기본 그룹 조회
    @Query("""
    select g from Groups g
    join fetch g.book
    join fetch g.host
    where g.groupId = :groupId
    and g.groupStatus != 'DELETED'
    """)
    Optional<Groups> findByIdWithBookAndHost(@Param("groupId") Long groupId);

    // 상태가 RECRUITING이고, 시작일(startDate)이 오늘이거나 이미 지난 그룹 조회
    @Query("""
        SELECT g
        FROM Groups g
        JOIN FETCH g.host
        JOIN FETCH g.book
        WHERE g.groupStatus = 'RECRUITING'
          AND g.startDate <= :today
    """)
    List<Groups> findGroupsToStart(@Param("today") LocalDate today);

    // 내가 방장인 그룹 중 '모집 중' 또는 '진행 중'인 개수
    long countByHostIdAndGroupStatusIn(Long hostId, List<GroupStatus> statuses);

    // 독서 종료일로부터 3일이 지났는데 아직 종료되지 않은(MATCHED) 그룹 조회
    @Query("SELECT g FROM Groups g WHERE g.groupStatus = 'MATCHED' " +
            "AND FUNCTION('DATE_ADD', g.startDate, g.readingPeriod) <= :deadline")
    List<Groups> findGroupsPastReviewDeadline(@Param("deadline") LocalDate deadline);
}
