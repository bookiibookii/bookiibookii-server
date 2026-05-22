package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.Tracker;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackerRepository extends JpaRepository<Tracker, Long> {

    @Query("SELECT t FROM Tracker t WHERE t.group.id = :groupId")
    Optional<Tracker> findByGroupId(@Param("groupId") Long groupId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tracker t WHERE t.group.id = :groupId")
    Optional<Tracker> findByGroupIdForUpdate(@Param("groupId") Long groupId);

    boolean existsByGroup_Id(Long groupId);

    @Query("SELECT t FROM Tracker t JOIN FETCH t.group WHERE t.group.id IN :groupIds")
    List<Tracker> findByGroup_IdIn(@Param("groupIds") List<Long> groupIds);

}
