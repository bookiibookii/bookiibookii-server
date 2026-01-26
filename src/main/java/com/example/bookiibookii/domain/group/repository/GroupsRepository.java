package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    @Query(value = "SELECT g FROM Groups g " +
            "WHERE g.groupId NOT IN :excludedIds " +
            "AND g.groupStatus = :status " +
            "ORDER BY RAND() " +
            "LIMIT :limit")
    List<Groups> findRandomGroupsExcluding(
            @Param("excludedIds") List<Long> excludedIds,
            @Param("status") GroupStatus status,
            @Param("limit") int limit
    );
}
