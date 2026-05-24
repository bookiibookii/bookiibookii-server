package com.example.bookiibookii.domain.groupbook.repository;

import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupBookRepository extends JpaRepository<GroupBook, Long> {

    List<GroupBook> findAllByGroup_Id(Long groupId);
}
