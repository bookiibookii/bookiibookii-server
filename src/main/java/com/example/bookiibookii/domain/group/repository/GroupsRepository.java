package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Groups;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupsRepository extends JpaRepository<Groups, Long> {
    // 1. 전체 모집 중인 그룹 조회 (Slice 적용)
    @Query("select g from Groups g " +
            //"join fetch g.book " +
            "join fetch g.host " +
            "where g.groupStatus = 'RECRUITING'")
    Slice<Groups> findAllWithBookAndHost(Pageable pageable);

    // 2. 지역별 필터링 조회 (Slice 적용)
    @Query("select g from Groups g " +
            //"join fetch g.book " +
            "join fetch g.host h " +
            "where h.region = :region " +
            "and g.tradeType = 'DIRECT' " +
            "and g.groupStatus != 'DELETED'")
    Slice<Groups> findAllByHostRegion(@Param("region") String region, Pageable pageable);
}
