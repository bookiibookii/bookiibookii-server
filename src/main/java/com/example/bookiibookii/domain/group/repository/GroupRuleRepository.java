package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.GroupRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GroupRuleRepository extends JpaRepository<GroupRule, Long> {
    @Query("SELECT gt FROM GroupRule gt WHERE gt.group.groupId IN :groupIds")
    List<GroupRule> findAllByGroupIdIn(@Param("groupIds") List<Long> groupIds);
}
