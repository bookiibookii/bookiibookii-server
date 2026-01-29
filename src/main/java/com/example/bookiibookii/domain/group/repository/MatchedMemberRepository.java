package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
        "WHERE g.groupId = :groupId AND mm.readingOrder = :readingOrder")
Optional<MatchedMember> findByGroupAndOrder(@Param("groupId") Long groupId, @Param("readingOrder") int readingOrder);

    //현재까지의 참여맴버 수
    long countByGroup(Groups groups);

    //참여맴버 리스트(읽는 순서대로 정렬)
    List<MatchedMember> findAllByGroupOrderByReadingOrderAsc(Groups group);
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

    interface WriterRow {
        Long getUserId();
        RoleStatus getRoleStatus();
    }

    // 참여한 그룹 중 상태가 RECRUITING, MATCHED 인 것 조회
    @Query("SELECT DISTINCT g FROM MatchedMember mm " +
            "JOIN mm.group g " +
            "JOIN FETCH g.book " +          // 책 정보 필요
            "LEFT JOIN FETCH g.groupTags gt " + // 태그 정보 필요
            "LEFT JOIN FETCH gt.tag " +
            "WHERE mm.user.id = :userId " +
            "AND g.groupStatus IN :statuses")
    List<Groups> findMyActiveGroups(
            @Param("userId") Long userId,
            @Param("statuses") List<GroupStatus> statuses
    );

    // 참여했던 전체 그룹 중 특정 타입(RELAY, TOGETHER) 개수 조회
    Long countByUserIdAndGroupGroupType(Long userId, GroupType groupType);

}