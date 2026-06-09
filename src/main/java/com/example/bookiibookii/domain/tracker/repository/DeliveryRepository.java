package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
