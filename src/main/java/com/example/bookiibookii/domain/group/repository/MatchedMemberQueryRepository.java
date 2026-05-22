package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.bookiibookii.domain.group.entity.QGroups.groups;
import static com.example.bookiibookii.domain.group.entity.QMatchedMember.matchedMember;
import static com.example.bookiibookii.domain.user.entity.QUser.user;

@Repository
@RequiredArgsConstructor
public class MatchedMemberQueryRepository {
    private final JPAQueryFactory queryFactory;

    // 유저가 속한 그룹 리스트 + host 정보 조회 (신고할 그룹 목록 드롭다운 조회)
    public List<GroupResponseDTO.GroupSummaryResponse> findGroupDtosByStatus(Long userId, GroupStatus status) {

        return queryFactory
                .select(Projections.constructor(GroupResponseDTO.GroupSummaryResponse.class,
                        groups.id, groups.book.title, groups.host.nickName,
                        // 호스트 여부
                        new CaseBuilder()
                                .when(groups.host.id.eq(userId)).then(true)
                                .otherwise(false)
                ))
                .from(matchedMember)
                .join(matchedMember.group, groups)
                .join(groups.host, user)
                .where(
                        matchedMember.user.id.eq(userId),
                        groups.groupStatus.eq(status)
                )
                .fetch();
    }

    // 특정 그룹의 신고할 멤버 목록 조회 (드롭다운)
    public List<GroupResponseDTO.GroupMemberResponse> findMemberDtosByGroupId(Long groupId, Long userId) {

        return queryFactory
                .select(Projections.constructor(GroupResponseDTO.GroupMemberResponse.class,
                        user.id,
                        user.nickName
                ))
                .from(matchedMember)
                .join(matchedMember.user, user) // 멤버 정보 가져오기 위해 Join
                .where(
                        matchedMember.group.id.eq(groupId),
                        user.id.ne(userId) // User 본인의 id는 제외
                )
                .fetch();
    }
}
