package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.location.repository.UserDeliveryRepository;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.entity.DeliveryAddress;
import com.example.bookiibookii.domain.tracker.dto.req.DeliveryRegisterRequestDTO;
import com.example.bookiibookii.domain.tracker.enums.DeliveryCompany;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.repository.DeliveryAddressRepository;
import com.example.bookiibookii.domain.tracker.repository.DeliveryRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.tracker.event.DeliveryNotificationEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PackageDeliveryServiceTest {

    @Mock
    private GroupsRepository groupsRepository;
    @Mock
    private MatchedMemberRepository matchedMemberRepository;
    @Mock
    private DeliveryAddressRepository deliveryAddressRepository;
    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private UserDeliveryRepository userDeliveryRepository;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private Clock clock;

    @InjectMocks
    private PackageDeliveryService packageDeliveryService;

    @BeforeEach
    void setUpClock() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-06-20T05:00:00Z"));
    }

    @Test
    void successfulRegistrationPublishesNotificationToDeliveryReceiver() {
        Groups group = Groups.builder()
                .id(10L)
                .tradeType(TradeType.DELIVERY)
                .groupStatus(GroupStatus.MATCHED)
                .build();
        MatchedMember sender = member(
                1L, 11L, group, ReadingStatus.EXCHANGING, ExchangeStatus.TRACKING_REGISTER_WAITING
        );
        MatchedMember receiver = member(
                2L, 22L, group, ReadingStatus.EXCHANGING, ExchangeStatus.TRACKING_REGISTER_WAITING
        );
        addBooks(sender, "발송할 책", "상대 책");
        addBooks(receiver, "상대 책", "발송할 책");
        DeliveryAddress senderAddress = address(group, sender);
        DeliveryAddress receiverAddress = address(group, receiver);

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(group.getId(), sender.getUser().getId()))
                .thenReturn(Optional.of(sender));
        when(matchedMemberRepository.findAllByGroup_Id(group.getId())).thenReturn(List.of(sender, receiver));
        when(deliveryAddressRepository.findByGroup_IdAndExchangeRoundAndMatchedMember_Id(
                group.getId(), ExchangeRound.FIRST_EXCHANGE, sender.getId()
        )).thenReturn(Optional.of(senderAddress));
        when(deliveryAddressRepository.findByGroup_IdAndExchangeRoundAndMatchedMember_Id(
                group.getId(), ExchangeRound.FIRST_EXCHANGE, receiver.getId()
        )).thenReturn(Optional.of(receiverAddress));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(invocation -> invocation.getArgument(0));

        packageDeliveryService.registerDelivery(
                group.getId(),
                new DeliveryRegisterRequestDTO(DeliveryCompany.CJ_LOGISTICS, "123456789"),
                sender.getUser()
        );

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        DeliveryNotificationEvent event = (DeliveryNotificationEvent) captor.getValue();
        assertThat(event.receiverId()).isEqualTo(receiver.getUser().getId());
        assertThat(event.actorId()).isEqualTo(sender.getUser().getId());
        assertThat(event.exchangeRound()).isEqualTo(ExchangeRound.FIRST_EXCHANGE);
        assertThat(event.bookTitle()).isEqualTo("발송할 책");
    }

    @Test
    void alreadyConfirmedDeliveryDoesNotPublishDuplicateConfirmation() {
        Groups group = Groups.builder()
                .id(11L)
                .tradeType(TradeType.DELIVERY)
                .groupStatus(GroupStatus.MATCHED)
                .build();
        MatchedMember me = member(1L, 10L, group, ReadingStatus.EXCHANGING, ExchangeStatus.RECEIVED_CONFIRMED);
        MatchedMember partner = member(2L, 20L, group, ReadingStatus.EXCHANGING, ExchangeStatus.TRACKING_REGISTERED);
        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(me, partner));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                packageDeliveryService.confirmPartnerDeliveryReceived(group.getId(), me.getUser())
        ).isInstanceOf(com.example.bookiibookii.domain.tracker.exception.TrackerException.class);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void duplicateRegistrationDoesNotPublishAnotherNotification() {
        Groups group = Groups.builder()
                .id(12L)
                .tradeType(TradeType.DELIVERY)
                .groupStatus(GroupStatus.MATCHED)
                .build();
        MatchedMember sender = member(
                1L, 11L, group, ReadingStatus.EXCHANGING, ExchangeStatus.TRACKING_REGISTER_WAITING
        );
        MatchedMember receiver = member(
                2L, 22L, group, ReadingStatus.EXCHANGING, ExchangeStatus.TRACKING_REGISTER_WAITING
        );
        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(group.getId(), sender.getUser().getId()))
                .thenReturn(Optional.of(sender));
        when(matchedMemberRepository.findAllByGroup_Id(group.getId())).thenReturn(List.of(sender, receiver));
        when(deliveryRepository.existsByGroup_IdAndExchangeRoundAndSender_Id(
                group.getId(), ExchangeRound.FIRST_EXCHANGE, sender.getId()
        )).thenReturn(true);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> packageDeliveryService.registerDelivery(
                group.getId(),
                new DeliveryRegisterRequestDTO(DeliveryCompany.CJ_LOGISTICS, "123456789"),
                sender.getUser()
        )).isInstanceOf(com.example.bookiibookii.domain.tracker.exception.TrackerException.class);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void returnDeliveryReceiveConfirmationKeepsTrackerOpenForMemberReview() {
        Groups group = Groups.builder()
                .id(1L)
                .tradeType(TradeType.DELIVERY)
                .groupStatus(GroupStatus.MATCHED)
                .build();
        MatchedMember me = member(1L, 10L, group, ExchangeStatus.TRACKING_REGISTERED);
        MatchedMember partner = member(2L, 20L, group, ExchangeStatus.RECEIVED_CONFIRMED);
        addBooks(me, "내 책", "상대 책");
        addBooks(partner, "상대 책", "내 책");
        me.changeCurrentMemberBook(findBook(me, false), Instant.now());
        partner.changeCurrentMemberBook(findBook(partner, false), Instant.now());
        findBook(me, true).updateCurrentPage(87);
        findBook(partner, true).updateCurrentPage(93);
        Delivery partnerDelivery = Delivery.builder()
                .id("delivery-1")
                .group(group)
                .exchangeRound(ExchangeRound.RETURN_EXCHANGE)
                .sender(partner)
                .receiver(me)
                .trackingRegisteredAt(Instant.now())
                .build();

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(me, partner));
        when(deliveryRepository.findByGroup_IdAndExchangeRoundAndSender_IdAndReceiver_Id(
                group.getId(),
                ExchangeRound.RETURN_EXCHANGE,
                partner.getId(),
                me.getId()
        )).thenReturn(Optional.of(partnerDelivery));

        packageDeliveryService.confirmPartnerDeliveryReceived(group.getId(), me.getUser());

        assertThat(group.getGroupStatus()).isEqualTo(GroupStatus.MATCHED);
        assertThat(me.getReadingStatus()).isEqualTo(ReadingStatus.PARTNER_REVIEWING);
        assertThat(partner.getReadingStatus()).isEqualTo(ReadingStatus.PARTNER_REVIEWING);
        assertThat(me.getExchangeStatus()).isEqualTo(ExchangeStatus.NOT_STARTED);
        assertThat(partner.getExchangeStatus()).isEqualTo(ExchangeStatus.NOT_STARTED);
        assertThat(me.getCompletedAt()).isNull();
        assertThat(partner.getCompletedAt()).isNull();
        assertThat(me.isReviewWritten()).isFalse();
        assertThat(partner.isReviewWritten()).isFalse();
        assertThat(partnerDelivery.getReceivedConfirmedAt()).isNotNull();
        assertThat(partnerDelivery.getDeliveredAt()).isNull();
        assertThat(me.getPartnerReviewingStartedAt()).isEqualTo(Instant.parse("2026-06-20T05:00:00Z"));
        assertThat(partner.getPartnerReviewingStartedAt()).isEqualTo(Instant.parse("2026-06-20T05:00:00Z"));
        assertThat(me.getCurrentMemberBook()).isSameAs(findBook(me, true));
        assertThat(partner.getCurrentMemberBook()).isSameAs(findBook(partner, true));
        assertThat(me.getCurrentMemberBook().getCurrentPage()).isEqualTo(87);
        assertThat(partner.getCurrentMemberBook().getCurrentPage()).isEqualTo(93);
    }

    @Test
    void firstDeliveryReceiveConfirmationAdvancesBothMembersToPartnerBookReading() {
        Groups group = Groups.builder()
                .id(2L)
                .tradeType(TradeType.DELIVERY)
                .groupStatus(GroupStatus.MATCHED)
                .build();
        MatchedMember me = member(3L, 30L, group, ReadingStatus.EXCHANGING, ExchangeStatus.TRACKING_REGISTERED);
        MatchedMember partner = member(4L, 40L, group, ReadingStatus.EXCHANGING, ExchangeStatus.RECEIVED_CONFIRMED);
        addBooks(me, "내 책", "상대 책");
        addBooks(partner, "상대 책", "내 책");
        findBook(me, false).updateCurrentPage(100);
        findBook(partner, false).updateCurrentPage(100);
        Delivery partnerDelivery = Delivery.builder()
                .id("delivery-2")
                .group(group)
                .exchangeRound(ExchangeRound.FIRST_EXCHANGE)
                .sender(partner)
                .receiver(me)
                .trackingRegisteredAt(Instant.now())
                .build();

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(me, partner));
        when(deliveryRepository.findByGroup_IdAndExchangeRoundAndSender_IdAndReceiver_Id(
                group.getId(),
                ExchangeRound.FIRST_EXCHANGE,
                partner.getId(),
                me.getId()
        )).thenReturn(Optional.of(partnerDelivery));

        packageDeliveryService.confirmPartnerDeliveryReceived(group.getId(), me.getUser());

        assertThat(me.getReadingStatus()).isEqualTo(ReadingStatus.PARTNER_BOOK_READING);
        assertThat(partner.getReadingStatus()).isEqualTo(ReadingStatus.PARTNER_BOOK_READING);
        assertThat(me.getExchangeStatus()).isEqualTo(ExchangeStatus.NOT_STARTED);
        assertThat(partner.getExchangeStatus()).isEqualTo(ExchangeStatus.NOT_STARTED);
        assertThat(me.getCurrentMemberBook()).isSameAs(findBook(me, false));
        assertThat(partner.getCurrentMemberBook()).isSameAs(findBook(partner, false));
        assertThat(me.getCurrentMemberBook().getCurrentPage()).isZero();
        assertThat(partner.getCurrentMemberBook().getCurrentPage()).isZero();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        DeliveryNotificationEvent event = (DeliveryNotificationEvent) captor.getValue();
        assertThat(event.receiverId()).isEqualTo(partner.getUser().getId());
        assertThat(event.actorId()).isEqualTo(me.getUser().getId());
        assertThat(event.deliveryId()).isEqualTo(partnerDelivery.getId());
        assertThat(event.bookTitle()).isEqualTo("상대 책");
    }

    private MatchedMember member(Long memberId, Long userId, Groups group, ExchangeStatus exchangeStatus) {
        return member(memberId, userId, group, ReadingStatus.RETURNING, exchangeStatus);
    }

    private MatchedMember member(
            Long memberId,
            Long userId,
            Groups group,
            ReadingStatus readingStatus,
            ExchangeStatus exchangeStatus
    ) {
        return MatchedMember.builder()
                .id(memberId)
                .group(group)
                .user(User.builder().id(userId).build())
                .readingStatus(readingStatus)
                .exchangeStatus(exchangeStatus)
                .build();
    }

    private void addBooks(MatchedMember member, String myTitle, String partnerTitle) {
        member.getMemberBooks().add(MemberBook.builder()
                .matchedMember(member)
                .book(Book.builder().title(myTitle).build())
                .isMine(true)
                .build());
        member.getMemberBooks().add(MemberBook.builder()
                .matchedMember(member)
                .book(Book.builder().title(partnerTitle).build())
                .isMine(false)
                .build());
    }

    private MemberBook findBook(MatchedMember member, boolean isMine) {
        return member.getMemberBooks().stream()
                .filter(memberBook -> memberBook.isMine() == isMine)
                .findFirst()
                .orElseThrow();
    }

    private DeliveryAddress address(Groups group, MatchedMember member) {
        return DeliveryAddress.builder()
                .group(group)
                .matchedMember(member)
                .exchangeRound(ExchangeRound.FIRST_EXCHANGE)
                .receiverName("수령자")
                .phoneNumber("01012345678")
                .address("주소")
                .zipCode("12345")
                .build();
    }
}
