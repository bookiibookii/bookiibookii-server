package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    boolean existsByGroup_IdAndExchangeRound(Long groupId, ExchangeRound exchangeRound);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select m
        from Meeting m
        join fetch m.group g
        join fetch m.createdBy cb
        join fetch cb.user
        where g.id = :groupId
          and m.exchangeRound = :exchangeRound
    """)
    Optional<Meeting> findByGroupIdAndExchangeRoundForUpdate(
            @Param("groupId") Long groupId,
            @Param("exchangeRound") ExchangeRound exchangeRound
    );

    @Query("""
        select m
        from Meeting m
        join fetch m.group g
        join fetch m.createdBy cb
        join fetch cb.user
        where g.id = :groupId
          and m.exchangeRound = :exchangeRound
    """)
    Optional<Meeting> findByGroupIdAndExchangeRound(
            @Param("groupId") Long groupId,
            @Param("exchangeRound") ExchangeRound exchangeRound
    );
}
