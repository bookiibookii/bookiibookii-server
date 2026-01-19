package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchedMemberRepository extends JpaRepository<MatchedMember, Long> {
    //특정 그룹에 현재 확정된 멤버 (방장포함) 몇명인지 계산
    long countByGroup(Groups groups);
}
