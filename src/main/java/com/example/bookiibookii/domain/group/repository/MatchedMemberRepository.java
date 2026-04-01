package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public interface MatchedMemberRepository extends JpaRepository<MatchedMember, Long> {

//    @Query("SELECT mm FROM MatchedMember mm " +
//            "JOIN FETCH mm.group g " +
//            "JOIN FETCH g.tracker t " +  // g.getTracker()를 탐색하는 방식
//            "WHERE mm.user.id = :userId")
//    List<MatchedMember> findAllByUserIdWithTracker(@Param("userId") Long userId);

    @Query("SELECT mm FROM MatchedMember mm " +
            "JOIN FETCH mm.group g " +
            "WHERE g.groupId = :groupId AND mm.matchedOrder = :matchedOrder")
    Optional<MatchedMember> findByGroupAndOrder(@Param("groupId") Long groupId, @Param("matchedOrder") int matchedOrder);

    //현재까지의 참여맴버 수
    long countByGroup(Groups groups);

    //참여맴버 리스트(읽는 순서대로 정렬) - User, UserImage fetch join으로 N+1 방지
    @Query("SELECT mm FROM MatchedMember mm " +
            "JOIN FETCH mm.user u " +
            "LEFT JOIN FETCH u.userImage " +
            "WHERE mm.group = :group " +
            "ORDER BY mm.matchedOrder ASC")
    List<MatchedMember> findAllByGroupOrderByMatchedOrderAsc(@Param("group") Groups group);
    //참여 취소를 위한 조회 메서드
    Optional<MatchedMember> findByGroup_GroupIdAndUser_Id(Long groupId, Long userId);

    // comment 도메인에서 사용
    @Query("""
    select mm.role
    from MatchedMember mm
    where mm.group.groupId = :groupId
      and mm.user.id = :userId
""")
    Optional<RoleStatus> findRoleByGroupIdAndUserId(@Param("groupId") Long groupId,
                                                    @Param("userId") Long userId);

    @Query("""
  select mm.user.id as userId,
         mm.role as roleStatus
  from MatchedMember mm
  where mm.group.groupId = :groupId
""")
    List<WriterRow> findWriterRowsByGroupId(@Param("groupId") Long groupId);

    List<MatchedMember> findAllByGroup_GroupId(Long groupId);


    interface WriterRow {
        Long getUserId();
        RoleStatus getRoleStatus();
    }

    // 그룹 내 파트너 matchedMember의 userId 반환 - 1:1 경우에서만 사용가능
    @Query("""
    select m.user.id
    from MatchedMember m
    where m.group.groupId = :groupId
      and m.user.id <> :actorId
""")
    Optional<Long> findPartnerUserId(@Param("groupId") Long groupId,
                                     @Param("actorId") Long actorId);

    // 유저가 그룹의 멤버인지 검증
    boolean existsByGroup_GroupIdAndUser_Id(Long groupId, Long userId);

    @Query("""
        select mm.user.id
        from MatchedMember mm
        where mm.group.groupId = :groupId
    """)
    
    List<Long> findMemberUserIdsByGroupId(@Param("groupId") Long groupId);
    // 참여한 그룹 중 상태가 RECRUITING, MATCHED 인 것 조회
    @Query("SELECT DISTINCT g FROM MatchedMember mm " +
            "JOIN mm.group g " +
            "JOIN FETCH g.book " +          // 책 정보 필요
            "LEFT JOIN FETCH g.groupTags gt " + // 태그 정보 필요
            "LEFT JOIN FETCH gt.tag " +
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
    long countByGroup_GroupIdAndIsReviewWrittenFalse(Long groupId);

    // 특정 그룹에서 리뷰 안 쓴 멤버 리스트 조회
    List<MatchedMember> findAllByGroup_GroupIdAndIsReviewWrittenFalse(Long groupId);

    @Query("select mm.user.id from MatchedMember mm where mm.group.groupId = :groupId")
    List<Long> findUserIdsByGroupId(@Param("groupId") Long groupId);
}