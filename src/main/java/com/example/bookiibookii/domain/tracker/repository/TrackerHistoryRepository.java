package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.entity.TrackerHistory;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackerHistoryRepository extends JpaRepository<TrackerHistory, Long> {

    // 특정 트래커의 모든 이력을 시간순(최신순)으로 가져올 때 사용.
    List<TrackerHistory> findAllByTrackerIdOrderByCreatedAtDesc(Long trackerId);

    // groupId를 통해 해당 그룹 트래커의 모든 히스토리 조회
    @Query("select h from TrackerHistory h " +
            "join h.tracker t " +
            "where t.group.groupId = :groupId " +
            "order by h.createdAt desc")
    List<TrackerHistory> findAllByGroupId(@Param("groupId") Long groupId);

    Optional<TrackerHistory> findTop1ByTracker_Group_GroupIdAndReceiverMatchedMemberIdOrderByCreatedAtDesc(
            Long groupId, Long receiverMatchedMemberId);

    @Query("SELECT th FROM TrackerHistory th " +
            "WHERE th.tracker = :tracker " +
            "AND th.trackerStatus IN :statuses " + // 파라미터로 받기
            "ORDER BY th.createdAt DESC LIMIT 1")
    Optional<TrackerHistory> findLatestShippingHistory(
            @Param("tracker") Tracker tracker,
            @Param("statuses") List<TrackerStatus> statuses // 상태 리스트 전달
    );

    @Query("SELECT th FROM TrackerHistory th " +
            "WHERE th.tracker.group.groupId = :groupId " +
            "AND th.senderMatchedMemberId = :senderId " + // 필드명과 일치시킴
            "ORDER BY th.createdAt DESC LIMIT 1")
    Optional<TrackerHistory> findLatestHistoryByGroupAndSender(
            @Param("groupId") Long groupId,
            @Param("senderId") Long senderId
    );

}
