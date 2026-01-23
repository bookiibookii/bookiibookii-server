package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.tag.enums.TagType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTagRepository extends JpaRepository<UserTag, Long> {
    void deleteAllByUser(User user);
    List<UserTag> findByUserIdAndTagTypeIn(Long userId, List<TagType> targetTypes);

    // 추천할 유저 후보 조회 (모집 중 그룹의 호스트 + 현재 유저 제외)
    @Query("SELECT ut FROM UserTag ut " +
            "JOIN FETCH ut.user u " +
            "JOIN FETCH ut.tag t " +
            "WHERE u.id <> :currentUserId " +
            "AND t.type IN :targetTypes " +
            "AND EXISTS (" +
            "  SELECT 1 FROM Groups g " +
            "  WHERE g.host.id = u.id " +
            "  AND g.groupStatus = :status)")
    List<UserTag> findRecommendUserTags(
            @Param("currentUserId") Long currentUserId,
            @Param("targetTypes") List<TagType> targetTypes,
            @Param("status") GroupStatus status
    );

}
