package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.enums.DeliveryStatus;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, String> {

    Optional<Delivery> findTopByGroup_GroupIdAndReceiver_IdAndDeliveryStatusOrderByCreatedAtDesc(
            Long groupId,
            Long receiverId,
            DeliveryStatus status
    );

    List<Delivery> findAllByGroup_GroupIdOrderByCreatedAtAsc(Long groupId);

    Optional<Delivery> findByGroup_GroupIdAndExchangeRoundAndSender_Id(
            Long groupId,
            ExchangeRound exchangeRound,
            Long senderId
    );

    Optional<Delivery> findByGroup_GroupIdAndExchangeRoundAndSender_IdAndReceiver_Id(
            Long groupId,
            ExchangeRound exchangeRound,
            Long senderId,
            Long receiverId
    );

    boolean existsByGroup_GroupIdAndExchangeRoundAndSender_Id(
            Long groupId,
            ExchangeRound exchangeRound,
            Long senderId
    );

    boolean existsByGroup_GroupIdAndExchangeRoundAndSender_IdAndReceiver_Id(
            Long groupId,
            ExchangeRound exchangeRound,
            Long senderId,
            Long receiverId
    );
}
