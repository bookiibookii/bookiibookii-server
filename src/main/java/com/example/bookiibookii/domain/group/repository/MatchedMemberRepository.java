package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public interface MatchedMemberRepository extends JpaRepository<MatchedMember, Long> {
    //현재까지의 참여맴버 수
    long countByGroup(Groups groups);

    //참여맴버 리스트(참여 시간 순서대로 정렬) - User, UserImage fetch join으로 N+1 방지
    @Query("SELECT mm FROM MatchedMember mm " +
            "JOIN FETCH mm.user u " +
            "LEFT JOIN FETCH u.userImage " +
            "WHERE mm.group = :group " +
            "ORDER BY mm.createdAt ASC")
    List<MatchedMember> findAllByGroupOrderByCreatedAtAsc(@Param("group") Groups group);
    //참여 취소를 위한 조회 메서드
    Optional<MatchedMember> findByGroup_IdAndUser_Id(Long groupId, Long userId);

    // comment 도메인에서 사용
    @Query("""
    select mm.role
    from MatchedMember mm
    where mm.group.id = :groupId
      and mm.user.id = :userId
""")
    Optional<RoleStatus> findRoleByGroupIdAndUserId(@Param("groupId") Long groupId,
                                                    @Param("userId") Long userId);

    @Query("""
  select mm.user.id as userId,
         mm.role as roleStatus
  from MatchedMember mm
  where mm.group.id = :groupId
""")
    List<WriterRow> findWriterRowsByGroupId(@Param("groupId") Long groupId);

    List<MatchedMember> findAllByGroup_Id(Long groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select distinct mm
        from MatchedMember mm
        join fetch mm.group g
        join fetch mm.user u
        left join fetch mm.currentMemberBook cmb
        left join fetch cmb.book
        left join fetch mm.memberBooks mb
        left join fetch mb.book
        where g.id = :groupId
    """)
    List<MatchedMember> findAllByGroupIdForUpdate(@Param("groupId") Long groupId);

    // 참여 시간 순 정렬 (TrackerService에서 순서 계산용)
    List<MatchedMember> findAllByGroup_IdOrderByCreatedAtAsc(Long groupId);

    // 가장 먼저 참여한 멤버 조회
    Optional<MatchedMember> findFirstByGroup_IdOrderByCreatedAtAsc(Long groupId);

    Optional<MatchedMember> findByGroup_IdAndRole(Long groupId, RoleStatus role);


    interface WriterRow {
        Long getUserId();
        RoleStatus getRoleStatus();
    }

    // 그룹 내 파트너 matchedMember의 userId 반환 - 1:1 경우에서만 사용가능
    @Query("""
    select m.user.id
    from MatchedMember m
    where m.group.id = :groupId
      and m.user.id <> :actorId
""")
    Optional<Long> findPartnerUserId(@Param("groupId") Long groupId,
                                     @Param("actorId") Long actorId);

    // 유저가 그룹의 멤버인지 검증
    boolean existsByGroup_IdAndUser_Id(Long groupId, Long userId);

    @Query("""
        select mm.user.id
        from MatchedMember mm
        where mm.group.id = :groupId
    """)
    
    List<Long> findMemberUserIdsByGroupId(@Param("groupId") Long groupId);
    // 참여한 그룹 중 상태가 RECRUITING, MATCHED 인 것 조회
    @Query("SELECT DISTINCT g FROM MatchedMember mm " +
            "JOIN mm.group g " +
            "JOIN FETCH g.book " +          // 책 정보 필요
            "WHERE mm.user.id = :userId AND mm.role = :roleStatus " +
            "AND g.groupStatus IN :statuses")
    List<Groups> findMyActiveGroups(
            @Param("userId") Long userId,
            @Param("statuses") List<GroupStatus> statuses,
            RoleStatus roleStatus
    );

    // 참여했던 전체 그룹 중 특정 타입(RELAY, TOGETHER) 개수 조회
    Long countByUser_IdAndGroup_GroupType(Long userId, GroupType groupType);

    // 내가 게스트로 '참여 확정'되어 진행 중인 그룹 개수
    long countByUserIdAndRoleAndGroup_GroupStatusIn(Long userId, RoleStatus role, List<GroupStatus> statuses);

    // 특정 그룹에서 아직 리뷰를 안 쓴 멤버 수 카운트
    long countByGroup_IdAndIsReviewWrittenFalse(Long groupId);

    // 특정 그룹에서 리뷰 안 쓴 멤버 리스트 조회
    List<MatchedMember> findAllByGroup_IdAndIsReviewWrittenFalse(Long groupId);

    @Query("select mm.user.id from MatchedMember mm where mm.group.id = :groupId")
    List<Long> findUserIdsByGroupId(@Param("groupId") Long groupId);

    @Query("""
        SELECT mm FROM MatchedMember mm
        JOIN FETCH mm.group
        WHERE mm.user.id = :userId
        AND mm.completedAt IS NOT NULL
    """)
    List<MatchedMember> findCompletedByUserId(@Param("userId") Long userId);

    @Query("""
        select distinct mm
        from MatchedMember mm
        join fetch mm.group g
        join fetch g.book
        join fetch mm.user u
        left join fetch u.userImage
        join fetch mm.currentMemberBook mcb
        join fetch mcb.book mb
        join fetch mcb.matchedMember mcbReader
        join fetch mcbReader.user mcbReaderUser
        left join fetch mcbReaderUser.userImage
        left join fetch g.matchedMember gm
        left join fetch gm.user gu
        left join fetch gu.userImage
        left join fetch gm.currentMemberBook pcb
        left join fetch pcb.book pb
        left join fetch pcb.matchedMember pcbReader
        left join fetch pcbReader.user pcbReaderUser
        left join fetch pcbReaderUser.userImage
        where mm.user.id = :memberId
          and mm.currentMemberBook is not null
          and g.groupStatus <> :completedGroupStatus
          and mm.readingStatus <> :completedReadingStatus
        order by mm.createdAt desc
    """)
    List<MatchedMember> findAllTrackerItemsByMemberId(
            @Param("memberId") Long memberId,
            @Param("completedGroupStatus") GroupStatus completedGroupStatus,
            @Param("completedReadingStatus") ReadingStatus completedReadingStatus
    );

    @Query("""
    select distinct mm
    from MatchedMember mm
    join fetch mm.group g
    join fetch mm.user u
    left join fetch mm.memberBooks mb
    left join fetch mb.book b
    join fetch mm.currentMemberBook cmb
    join fetch cmb.book cb
    where g.id = :groupId
""")
    List<MatchedMember> findAllTrackerMembersByGroupId(@Param("groupId") Long groupId);
}
