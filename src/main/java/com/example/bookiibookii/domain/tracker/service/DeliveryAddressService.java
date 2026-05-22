package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.entity.GroupPlace;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupPlaceSourceType;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.repository.GroupPlaceRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.location.entity.UserDelivery;
import com.example.bookiibookii.domain.location.repository.UserDeliveryRepository;
import com.example.bookiibookii.domain.tracker.entity.DeliveryAddress;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.tracker.repository.DeliveryAddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryAddressService {

    private final DeliveryAddressRepository deliveryAddressRepository;
    private final GroupPlaceRepository groupPlaceRepository;
    private final UserDeliveryRepository userDeliveryRepository;
    private final MatchedMemberRepository matchedMemberRepository;

    @Transactional
    public void createFirstExchangeAddressesIfAbsent(Long groupId) {
        List<MatchedMember> members = matchedMemberRepository.findAllByGroupIdForUpdate(groupId);
        if (members.size() != 2) {
            throw new TrackerException(TrackerErrorCode.INVALID_PARTNER_COUNT);
        }

        for (MatchedMember member : members) {
            if (deliveryAddressRepository.existsByGroup_IdAndExchangeRoundAndMatchedMember_Id(
                    groupId,
                    ExchangeRound.FIRST_EXCHANGE,
                    member.getId()
            )) {
                continue;
            }

            DeliveryAddress snapshot = member.getRole() == RoleStatus.HOST
                    ? createHostSnapshot(member)
                    : createGuestSnapshot(member);

            try {
                deliveryAddressRepository.save(snapshot);
            } catch (DataIntegrityViolationException e) {
                e.getMostSpecificCause();
                if (!e.getMostSpecificCause().getMessage().contains("uk_delivery_address_group_round_member")) {
                    throw e;
                }
            }
        }
    }

    private DeliveryAddress createHostSnapshot(MatchedMember host) {
        GroupPlace placeSnapshot = groupPlaceRepository
                .findByGroup_Id(host.getGroup().getId())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.GROUP_SELECTED_PLACE_NOT_FOUND));

        if (placeSnapshot.getSourceType() != GroupPlaceSourceType.USER_DELIVERY
                || isBlank(placeSnapshot.getReceiverName())
                || isBlank(placeSnapshot.getPhoneNumber())
                || isBlank(placeSnapshot.getAddress())
                || isBlank(placeSnapshot.getZipCode())) {
            throw new TrackerException(TrackerErrorCode.INVALID_GROUP_DELIVERY_PLACE);
        }

        return DeliveryAddress.builder()
                .group(host.getGroup())
                .matchedMember(host)
                .exchangeRound(ExchangeRound.FIRST_EXCHANGE)
                .receiverName(placeSnapshot.getReceiverName())
                .phoneNumber(placeSnapshot.getPhoneNumber())
                .address(placeSnapshot.getAddress())
                .addressDetail(placeSnapshot.getAddressDetail())
                .zipCode(placeSnapshot.getZipCode())
                .build();
    }

    private DeliveryAddress createGuestSnapshot(MatchedMember guest) {
        UserDelivery userDelivery = userDeliveryRepository
                .findFirstByUser_IdOrderByCreatedAtAsc(guest.getUser().getId())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.DELIVERY_ADDRESS_NOT_FOUND));

        return DeliveryAddress.builder()
                .group(guest.getGroup())
                .matchedMember(guest)
                .exchangeRound(ExchangeRound.FIRST_EXCHANGE)
                .receiverName(userDelivery.getReceiverName())
                .phoneNumber(userDelivery.getPhone())
                .address(userDelivery.getLocation().getAddress())
                .addressDetail(userDelivery.getAddressDetail())
                .zipCode(userDelivery.getLocation().getZipCode())
                .build();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
