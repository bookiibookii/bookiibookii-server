package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, String> {

    List<Delivery> findAllByGroup_IdOrderByCreatedAtAsc(Long groupId);

    Optional<Delivery> findByGroup_IdAndExchangeRoundAndSender_Id(
            Long groupId,
            ExchangeRound exchangeRound,
            Long senderId
    );

    Optional<Delivery> findByGroup_IdAndExchangeRoundAndSender_IdAndReceiver_Id(
            Long groupId,
            ExchangeRound exchangeRound,
            Long senderId,
            Long receiverId
    );

    boolean existsByGroup_IdAndExchangeRoundAndSender_Id(
            Long groupId,
            ExchangeRound exchangeRound,
            Long senderId
    );

    boolean existsByGroup_IdAndExchangeRoundAndSender_IdAndReceiver_Id(
            Long groupId,
            ExchangeRound exchangeRound,
            Long senderId,
            Long receiverId
    );

    @Query("""
        select distinct d from Delivery d
        join fetch d.group g
        join fetch d.sender sender
        join fetch sender.user
        join fetch sender.memberBooks senderBook
        join fetch senderBook.book
        join fetch d.receiver receiver
        join fetch receiver.user
        where d.createdAt <= :cutoff
          and d.receivedConfirmedAt is null
          and g.tradeType = com.example.bookiibookii.domain.group.enums.TradeType.DELIVERY
          and g.groupStatus = com.example.bookiibookii.domain.group.enums.GroupStatus.MATCHED
          and (
               (d.exchangeRound = com.example.bookiibookii.domain.tracker.enums.ExchangeRound.FIRST_EXCHANGE
                and sender.readingStatus = com.example.bookiibookii.domain.tracker.enums.ReadingStatus.EXCHANGING
                and receiver.readingStatus = com.example.bookiibookii.domain.tracker.enums.ReadingStatus.EXCHANGING)
            or (d.exchangeRound = com.example.bookiibookii.domain.tracker.enums.ExchangeRound.RETURN_EXCHANGE
                and sender.readingStatus = com.example.bookiibookii.domain.tracker.enums.ReadingStatus.RETURNING
                and receiver.readingStatus = com.example.bookiibookii.domain.tracker.enums.ReadingStatus.RETURNING)
          )
    """)
    List<Delivery> findReceiveReminderCandidates(@Param("cutoff") LocalDateTime cutoff);
}
