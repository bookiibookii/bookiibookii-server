package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.GroupTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupTagRepository extends JpaRepository<GroupTag, Long> {
    //
    @Query("SELECT gt FROM GroupTag gt JOIN FETCH gt.tag WHERE gt.group.groupId IN :groupIds")
    default List<GroupTag> findAllByGroupIdIn(@Param("groupIds") List<Long> groupIds) {
        return null;
    }
}