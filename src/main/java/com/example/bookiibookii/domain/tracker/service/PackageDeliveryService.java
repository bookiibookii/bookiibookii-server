package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.tracker.dto.req.DeliveryAddressUpdateRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.DeliveryRegisterRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.DeliveryAddressResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.PartnerDeliveryResponseDTO;
import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.entity.DeliveryAddress;
import com.example.bookiibookii.domain.tracker.enums.DeliveryStatus;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.tracker.repository.DeliveryAddressRepository;
import com.example.bookiibookii.domain.tracker.repository.DeliveryRepository;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PackageDeliveryService {

    private static final String TRACKING_NUMBER_PATTERN = "\\d+";

    private final GroupsRepository groupsRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final DeliveryRepository deliveryRepository;

    public DeliveryAddressResponseDTO getAddresses(Long groupId, User user) {
        Groups group = validatePackageGroup(groupId);
        MatchedMember me = getMyMatchedMember(groupId, user.getId());
        MatchedMember partner = getPartnerMember(groupId, me.getId());
        ExchangeRound currentExchangeRound = validatePackageExchangeStage(me);

        DeliveryAddress myAddress = getAddressSnapshot(group.getId(), currentExchangeRound, me.getId());
        DeliveryAddress partnerAddress = getAddressSnapshot(group.getId(), currentExchangeRound, partner.getId());

        return DeliveryAddressResponseDTO.builder()
                .myAddress(DeliveryAddressResponseDTO.AddressDTO.from(myAddress))
                .partnerAddress(DeliveryAddressResponseDTO.AddressDTO.from(partnerAddress))
                .canEditMyAddress(canEditAddress(groupId, currentExchangeRound, me, partner))
                .build();
    }

    @Transactional
    public DeliveryAddressResponseDTO updateMyAddress(Long groupId, DeliveryAddressUpdateRequestDTO request, User user) {
        Groups group = validatePackageGroupForUpdate(groupId);
        MatchedMember me = getMyMatchedMember(groupId, user.getId());
        MatchedMember partner = getPartnerMember(groupId, me.getId());
        ExchangeRound currentExchangeRound = validatePackageExchangeStage(me);
        validateAddressEditable(groupId, currentExchangeRound, me, partner);

        DeliveryAddress myAddress = getAddressSnapshot(group.getId(), currentExchangeRound, me.getId());
        myAddress.update(
                request.receiverName(),
                request.phoneNumber(),
                request.address(),
                request.addressDetail(),
                request.zipCode()
        );

        DeliveryAddress partnerAddress = getAddressSnapshot(group.getId(), currentExchangeRound, partner.getId());
        return DeliveryAddressResponseDTO.builder()
                .myAddress(DeliveryAddressResponseDTO.AddressDTO.from(myAddress))
                .partnerAddress(DeliveryAddressResponseDTO.AddressDTO.from(partnerAddress))
                .canEditMyAddress(canEditAddress(groupId, currentExchangeRound, me, partner))
                .build();
    }

    @Transactional
    public void registerDelivery(Long groupId, DeliveryRegisterRequestDTO request, User user) {
        Groups group = validatePackageGroupForUpdate(groupId);
        MatchedMember me = getMyMatchedMember(groupId, user.getId());
        MatchedMember partner = getPartnerMember(groupId, me.getId());
        ExchangeRound currentExchangeRound = validatePackageExchangeStage(me);

        if (me.getExchangeStatus() != ExchangeStatus.TRACKING_REGISTER_WAITING) {
            throw new TrackerException(TrackerErrorCode.DELIVERY_ADDRESS_CANNOT_BE_CHANGED);
        }
        if (deliveryRepository.existsByGroup_IdAndExchangeRoundAndSender_Id(groupId, currentExchangeRound, me.getId())) {
            throw new TrackerException(TrackerErrorCode.DELIVERY_ALREADY_REGISTERED);
        }
        if (request.deliveryCompany() == null) {
            throw new TrackerException(TrackerErrorCode.INVALID_DELIVERY_COMPANY);
        }
        if (request.trackingNumber() == null || !request.trackingNumber().matches(TRACKING_NUMBER_PATTERN)) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKING_NUMBER);
        }

        getAddressSnapshot(groupId, currentExchangeRound, me.getId());
        getAddressSnapshot(groupId, currentExchangeRound, partner.getId());

        Delivery delivery = Delivery.builder()
                .id(UUID.randomUUID().toString())
                .group(group)
                .exchangeRound(currentExchangeRound)
                .deliveryStatus(DeliveryStatus.SHIPPING)
                .sender(me)
                .receiver(partner)
                .deliveryCompany(request.deliveryCompany())
                .trackingNumber(request.trackingNumber())
                .startDate(LocalDateTime.now())
                .build();

        try {
            deliveryRepository.save(delivery);
        } catch (DataIntegrityViolationException e) {
            throw new TrackerException(TrackerErrorCode.DELIVERY_ALREADY_REGISTERED);
        }

        me.updateExchangeStatus(ExchangeStatus.TRACKING_REGISTERED);
    }

    public PartnerDeliveryResponseDTO getPartnerDelivery(Long groupId, User user) {
        validatePackageGroup(groupId);
        MatchedMember me = getMyMatchedMember(groupId, user.getId());
        MatchedMember partner = getPartnerMember(groupId, me.getId());
        ExchangeRound currentExchangeRound = validatePackageExchangeStage(me);

        if (me.getExchangeStatus() != ExchangeStatus.TRACKING_REGISTERED
                && me.getExchangeStatus() != ExchangeStatus.RECEIVED_CONFIRMED) {
            throw new TrackerException(TrackerErrorCode.NOT_EXCHANGE_STAGE);
        }

        Delivery partnerDelivery = getPartnerToMeDelivery(groupId, currentExchangeRound, partner, me);
        boolean canConfirmReceived = me.getExchangeStatus() == ExchangeStatus.TRACKING_REGISTERED
                && partnerDelivery.getReceivedConfirmedAt() == null;
        return PartnerDeliveryResponseDTO.of(partnerDelivery, canConfirmReceived);
    }

    @Transactional
    public void confirmPartnerDeliveryReceived(Long groupId, User user) {
        validatePackageGroupForUpdate(groupId);
        List<MatchedMember> members = matchedMemberRepository.findAllByGroupIdForUpdate(groupId);
        MatchedMember me = findMe(members, user.getId());
        MatchedMember partner = findPartner(members, me.getId());
        ExchangeRound currentExchangeRound = validatePackageExchangeStage(me);

        ExchangeStatus exchangeStatus = me.getExchangeStatus();
        if (exchangeStatus == ExchangeStatus.RECEIVED_CONFIRMED) {
            throw new TrackerException(TrackerErrorCode.DELIVERY_ALREADY_RECEIVED);
        }
        if (exchangeStatus != ExchangeStatus.TRACKING_REGISTERED) {
            throw new TrackerException(TrackerErrorCode.NOT_EXCHANGE_STAGE);
        }

        Delivery partnerDelivery = getPartnerToMeDelivery(groupId, currentExchangeRound, partner, me);
        if (partnerDelivery.getReceivedConfirmedAt() != null) {
            throw new TrackerException(TrackerErrorCode.DELIVERY_ALREADY_RECEIVED);
        }

        partnerDelivery.confirmReceived(LocalDateTime.now());
        me.updateExchangeStatus(ExchangeStatus.RECEIVED_CONFIRMED);

        if (members.stream().allMatch(member -> member.getExchangeStatus() == ExchangeStatus.RECEIVED_CONFIRMED)) {
            LocalDateTime now = LocalDateTime.now();
            if (currentExchangeRound == ExchangeRound.RETURN_EXCHANGE) {
                members.forEach(member -> member.updateExchangeStatus(ExchangeStatus.NOT_STARTED));
                return;
            }

            members.forEach(member -> {
                member.changeCurrentMemberBook(findPartnerBook(member), now);
                member.updateReadingStatus(ReadingStatus.PARTNER_BOOK_READING);
                member.updateExchangeStatus(ExchangeStatus.NOT_STARTED);
            });
        }
    }

    // 내부 메서드
    private Groups validatePackageGroup(Long groupId) {
        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        if (group.getTradeType() != TradeType.DELIVERY) {
            throw new TrackerException(TrackerErrorCode.NOT_PACKAGE_TRADE_GROUP);
        }
        return group;
    }

    private Groups validatePackageGroupForUpdate(Long groupId) {
        Groups group = groupsRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        if (group.getTradeType() != TradeType.DELIVERY) {
            throw new TrackerException(TrackerErrorCode.NOT_PACKAGE_TRADE_GROUP);
        }
        return group;
    }

    private MatchedMember getMyMatchedMember(Long groupId, Long userId) {
        return matchedMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));
    }

    private MatchedMember getPartnerMember(Long groupId, Long myMemberId) {
        return matchedMemberRepository.findAllByGroup_Id(groupId).stream()
                .filter(member -> !member.getId().equals(myMemberId))
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.PARTNER_NOT_FOUND));
    }

    private MatchedMember findMe(List<MatchedMember> members, Long userId) {
        return members.stream()
                .filter(member -> member.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));
    }

    private MatchedMember findPartner(List<MatchedMember> members, Long myMemberId) {
        return members.stream()
                .filter(member -> !member.getId().equals(myMemberId))
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.PARTNER_NOT_FOUND));
    }

    private ExchangeRound validatePackageExchangeStage(MatchedMember me) {
        return switch (me.getReadingStatus()) {
            case EXCHANGING -> ExchangeRound.FIRST_EXCHANGE;
            case RETURNING -> ExchangeRound.RETURN_EXCHANGE;
            default -> throw new TrackerException(TrackerErrorCode.NOT_EXCHANGE_STAGE);
        };
    }

    private DeliveryAddress getAddressSnapshot(Long groupId, ExchangeRound exchangeRound, Long matchedMemberId) {
        return deliveryAddressRepository
                .findByGroup_IdAndExchangeRoundAndMatchedMember_Id(groupId, exchangeRound, matchedMemberId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.DELIVERY_ADDRESS_NOT_FOUND));
    }

    private boolean canEditAddress(Long groupId, ExchangeRound exchangeRound, MatchedMember me, MatchedMember partner) {
        return me.getExchangeStatus() == ExchangeStatus.TRACKING_REGISTER_WAITING
                && !deliveryRepository.existsByGroup_IdAndExchangeRoundAndSender_Id(groupId, exchangeRound, me.getId())
                && !deliveryRepository.existsByGroup_IdAndExchangeRoundAndSender_IdAndReceiver_Id(
                groupId,
                exchangeRound,
                partner.getId(),
                me.getId()
        );
    }

    private void validateAddressEditable(Long groupId, ExchangeRound exchangeRound, MatchedMember me, MatchedMember partner) {
        if (me.getExchangeStatus() != ExchangeStatus.TRACKING_REGISTER_WAITING) {
            throw new TrackerException(TrackerErrorCode.DELIVERY_ADDRESS_CANNOT_BE_CHANGED);
        }
        if (deliveryRepository.existsByGroup_IdAndExchangeRoundAndSender_Id(groupId, exchangeRound, me.getId())
                || deliveryRepository.existsByGroup_IdAndExchangeRoundAndSender_IdAndReceiver_Id(
                groupId,
                exchangeRound,
                partner.getId(),
                me.getId()
        )) {
            throw new TrackerException(TrackerErrorCode.DELIVERY_ADDRESS_ALREADY_USED);
        }
    }

    private Delivery getPartnerToMeDelivery(
            Long groupId,
            ExchangeRound exchangeRound,
            MatchedMember partner,
            MatchedMember me
    ) {
        return deliveryRepository
                .findByGroup_IdAndExchangeRoundAndSender_IdAndReceiver_Id(
                        groupId,
                        exchangeRound,
                        partner.getId(),
                        me.getId()
                )
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.PARTNER_DELIVERY_NOT_REGISTERED));
    }

    private MemberBook findPartnerBook(MatchedMember matchedMember) {
        return matchedMember.getMemberBooks().stream()
                .filter(memberBook -> !memberBook.isMine())
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.INVALID_CURRENT_MEMBER_BOOK));
    }
}
