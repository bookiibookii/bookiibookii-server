package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.GroupPlace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupPlaceRepository extends JpaRepository<GroupPlace, Long> {

    Optional<GroupPlace> findByGroup_GroupId(Long groupId);
}
