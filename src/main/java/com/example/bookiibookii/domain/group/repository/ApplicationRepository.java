package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Application;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    // N+1 문제 해결: 신청서 + 신청자(User) + 유저의 태그들까지
    @Query("SELECT a DISTINCT FROM Application a " +
            "JOIN FETCH a.guest g " +
            "LEFT JOIN FETCH g.userTags ut " +
            "LEFT JOIN FETCH ut.tag " +
            "WHERE a.group.groupId = :groupId AND a.applicationStatus = 'PENDING'")
    List<Application> findAllWithGuestAndTagsByGroupId(@Param("groupId") Long groupId);

    // 특정 그룹의 현재 수락된 인원수를 세기 위한 메서드
    long countByGroupGroupIdAndApplicationStatus(Long groupId, ApplicationStatus status);

    //특정 그룹의 '대기 중'인 신청서들만 조회 (Fetch Join으로 Guest 정보까지 한 번에)
    @Query("SELECT a FROM Application a JOIN FETCH a.guest WHERE a.group.id = :groupId AND a.applicationStatus = 'PENDING'")
    List<Application> findAllPendingByGroupId(@Param("groupId") Long groupId);

    // 중복 신청 확인: 특정 그룹에 특정 유저가 이미 신청했는지 여부
    boolean existsByGroupGroupIdAndGuestId(Long groupId, Long guestId);

    //신청 상태값 조회메서드
    boolean existsByGroupGroupIdAndGuestIdAndApplicationStatus(
            Long groupId,
            Long guestId,
            ApplicationStatus status
    );
}
