package com.example.bookiibookii.domain.groupbook.repository;

import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupBookRepository extends JpaRepository<GroupBook, Long> {

    @Query("SELECT ub FROM GroupBook ub JOIN FETCH ub.user JOIN FETCH ub.group WHERE ub.group.id IN :groupIds")
    List<GroupBook> findByGroup_IdInWithUserAndGroup(@Param("groupIds") List<Long> groupIds);

    List<GroupBook> findAllByGroup_Id(Long groupId);
}
