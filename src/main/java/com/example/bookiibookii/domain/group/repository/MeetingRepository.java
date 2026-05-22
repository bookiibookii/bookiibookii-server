package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Meeting;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    boolean existsByGroup_Id(Long groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select m
        from Meeting m
        join fetch m.group g
        join fetch m.createdBy cb
        join fetch cb.user
        join fetch m.location
        where g.id = :groupId
    """)
    Optional<Meeting> findByGroupIdForUpdate(@Param("groupId") Long groupId);

    @Query("""
        select m
        from Meeting m
        join fetch m.group g
        join fetch m.createdBy cb
        join fetch cb.user
        join fetch m.location
        where g.id = :groupId
    """)
    Optional<Meeting> findByGroupId(@Param("groupId") Long groupId);
}
