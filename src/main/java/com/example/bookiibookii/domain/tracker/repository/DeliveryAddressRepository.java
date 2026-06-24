package com.example.bookiibookii.domain.tracker.repository;

import com.example.bookiibookii.domain.tracker.entity.DeliveryAddress;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {

    Optional<DeliveryAddress> findByGroup_IdAndExchangeRoundAndMatchedMember_Id(
            Long groupId,
            ExchangeRound exchangeRound,
            Long matchedMemberId
    );

    boolean existsByGroup_IdAndExchangeRoundAndMatchedMember_Id(
            Long groupId,
            ExchangeRound exchangeRound,
            Long matchedMemberId
    );
}
