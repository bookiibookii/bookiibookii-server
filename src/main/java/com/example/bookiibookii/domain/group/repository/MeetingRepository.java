package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    @Query("""
        select count(m) > 0
        from Meeting m
        where m.group.id = :groupId
          and m.exchangeRound = :exchangeRound
    """)
    boolean existsByGroupIdAndExchangeRound(
            @Param("groupId") Long groupId,
            @Param("exchangeRound") ExchangeRound exchangeRound
    );

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

    @Query("""
        select m
        from Meeting m
        where m.group.id in :groupIds
    """)
    List<Meeting> findAllByGroupIds(@Param("groupIds") List<Long> groupIds);

    @Query("""
        select distinct m
        from Meeting m
        join fetch m.group g
        join fetch g.book
        where g.tradeType = :tradeType
          and g.groupStatus = :groupStatus
          and m.meetingAt <= :cutoff
          and exists (
              select mm.id
              from MatchedMember mm
              where mm.group = g
                and mm.exchangeStatus = :scheduledStatus
                and (
                    (
                        m.exchangeRound = :firstRound
                        and mm.readingStatus = :firstReadingStatus
                        and not exists (
                            select other.id
                            from MatchedMember other
                            where other.group = g
                              and (
                                  other.readingStatus is null
                                  or other.readingStatus <> :firstReadingStatus
                              )
                        )
                    )
                    or
                    (
                        m.exchangeRound = :returnRound
                        and mm.readingStatus = :returnReadingStatus
                        and not exists (
                            select other.id
                            from MatchedMember other
                            where other.group = g
                              and (
                                  other.readingStatus is null
                                  or other.readingStatus <> :returnReadingStatus
                              )
                        )
                    )
                )
          )
    """)
    List<Meeting> findDueDirectMeetingReminders(
            @Param("cutoff") Instant cutoff,
            @Param("tradeType") TradeType tradeType,
            @Param("groupStatus") GroupStatus groupStatus,
            @Param("scheduledStatus") ExchangeStatus scheduledStatus,
            @Param("firstRound") ExchangeRound firstRound,
            @Param("firstReadingStatus") ReadingStatus firstReadingStatus,
            @Param("returnRound") ExchangeRound returnRound,
            @Param("returnReadingStatus") ReadingStatus returnReadingStatus
    );

}
