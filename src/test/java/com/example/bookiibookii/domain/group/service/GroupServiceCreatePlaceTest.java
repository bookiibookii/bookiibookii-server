package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.book.repository.BookRepository;
import com.example.bookiibookii.domain.book.service.BookService;
import com.example.bookiibookii.domain.group.dto.RuleDTO;
import com.example.bookiibookii.domain.group.dto.req.GroupRequestDTO;
import com.example.bookiibookii.domain.group.entity.GroupPlace;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.GroupPlaceSourceType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.ApplicationRepository;
import com.example.bookiibookii.domain.group.repository.GroupPlaceRepository;
import com.example.bookiibookii.domain.group.repository.GroupQueryRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberQueryRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.location.entity.UserDelivery;
import com.example.bookiibookii.domain.location.entity.UserExchange;
import com.example.bookiibookii.domain.location.repository.UserDeliveryRepository;
import com.example.bookiibookii.domain.location.repository.UserExchangeRepository;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.notification.service.KeywordMatchService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.Tag;
import com.example.bookiibookii.domain.user.service.BadWordService;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.global.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GroupServiceCreatePlaceTest {

    @Mock private GroupsRepository groupsRepository;
    @Mock private MatchedMemberRepository matchedMemberRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private BookService bookService;
    @Mock private GroupQueryRepository groupQueryRepository;
    @Mock private KeywordMatchService keywordMatchService;
    @Mock private DomainEventPublisher publisher;
    @Mock private MatchedMemberQueryRepository matchedMemberQueryRepository;
    @Mock private UserImageS3Service userImageS3Service;
    @Mock private RedisUtil redisUtil;
    @Mock private MeetingRepository meetingRepository;
    @Mock private BadWordService badWordService;
    @Mock private UserExchangeRepository userExchangeRepository;
    @Mock private UserDeliveryRepository userDeliveryRepository;
    @Mock private GroupPlaceRepository groupPlaceRepository;
    @Mock private BookRepository bookRepository;

    @InjectMocks
    private GroupService groupService;

    private final User host = User.builder().id(1L).nickName("host").build();
    private final Book book = Book.builder()
            .id(10L)
            .isbn13("9791167527721")
            .title("경제 책")
            .author("저자")
            .publisher("출판사")
            .image("https://example.com/book.jpg")
            .totalPages(300)
            .link("https://example.com")
            .category(CustomCategory.ECONOMY_BUSINESS)
            .build();

    @BeforeEach
    void setUp() {
        when(groupsRepository.countByHostIdAndGroupStatusIn(any(), any())).thenReturn(0L);
        lenient().when(bookService.getOrCreateByIsbn13("9791167527721")).thenReturn(book);
        lenient().when(groupsRepository.save(any(Groups.class))).thenAnswer(invocation -> {
            Groups group = invocation.getArgument(0);
            group.setId(100L);
            return group;
        });
    }

    @Test
    void createsDirectGroupFromUserExchangeId() {
        GroupRequestDTO.CreateDTO request = request(TradeType.DIRECT, null, 1L);
        UserExchange exchange = UserExchange.builder()
                .id(1L)
                .user(host)
                .location(exchangeLocation())
                .addressDetail("중앙 입구")
                .build();
        when(userExchangeRepository.findById(1L)).thenReturn(Optional.of(exchange));
        when(keywordMatchService.matchForGroup(
                book.getTitle(), book.getAuthor(), request.getGroupName()
        )).thenReturn(List.of());

        groupService.createGroup(host, request);

        ArgumentCaptor<GroupPlace> captor = ArgumentCaptor.forClass(GroupPlace.class);
        verify(groupPlaceRepository).save(captor.capture());
        GroupPlace snapshot = captor.getValue();
        assertThat(snapshot.getSourceType()).isEqualTo(GroupPlaceSourceType.USER_EXCHANGE);
        assertThat(snapshot.getPlaceName()).isEqualTo("숭실대입구역");
        assertThat(snapshot.getAddress()).isEqualTo("서울특별시 동작구 상도로 378");
        assertThat(snapshot.getX()).isEqualByComparingTo("126.9534");
        assertThat(snapshot.getY()).isEqualByComparingTo("37.4963");
        assertThat(snapshot.getReceiverName()).isNull();
        assertThat(snapshot.getPhoneNumber()).isNull();
        verifyNoInteractions(userDeliveryRepository);
    }

    @Test
    void failsDirectGroupCreationWhenUserDeliveryIdIsPassedInsteadOfUserExchangeId() {
        assertThatThrownBy(() -> groupService.createGroup(host, request(TradeType.DIRECT, 1L, null)))
                .isInstanceOfSatisfying(GroupException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(GroupErrorCode.USER_DELIVERY_ID_NOT_ALLOWED_FOR_DIRECT));

        verifyNoInteractions(userExchangeRepository);
        verifyNoInteractions(userDeliveryRepository);
        verify(groupPlaceRepository, never()).save(any());
    }

    @Test
    void failsDirectGroupCreationWhenUserExchangeIdIsMissing() {
        assertThatThrownBy(() -> groupService.createGroup(host, request(TradeType.DIRECT, null, null)))
                .isInstanceOfSatisfying(GroupException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(GroupErrorCode.USER_EXCHANGE_ID_REQUIRED));

        verifyNoInteractions(userExchangeRepository);
        verifyNoInteractions(userDeliveryRepository);
        verify(groupPlaceRepository, never()).save(any());
    }

    @Test
    void failsDirectGroupCreationWhenExchangePlaceDoesNotExist() {
        when(userExchangeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.createGroup(host, request(TradeType.DIRECT, null, 1L)))
                .isInstanceOfSatisfying(GroupException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(GroupErrorCode.DIRECT_EXCHANGE_PLACE_NOT_FOUND));

        verifyNoInteractions(userDeliveryRepository);
        verify(groupPlaceRepository, never()).save(any());
    }

    @Test
    void failsDirectGroupCreationWhenExchangePlaceBelongsToAnotherUser() {
        User otherUser = User.builder().id(2L).build();
        UserExchange exchange = UserExchange.builder()
                .id(1L)
                .user(otherUser)
                .location(exchangeLocation())
                .build();
        when(userExchangeRepository.findById(1L)).thenReturn(Optional.of(exchange));

        assertThatThrownBy(() -> groupService.createGroup(host, request(TradeType.DIRECT, null, 1L)))
                .isInstanceOfSatisfying(GroupException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(GroupErrorCode.NOT_MY_EXCHANGE_PLACE));
    }

    @Test
    void createsDeliveryGroupFromUserDeliveryId() {
        GroupRequestDTO.CreateDTO request = request(TradeType.DELIVERY, 1L, null);
        UserDelivery delivery = UserDelivery.builder()
                .id(1L)
                .user(host)
                .location(deliveryLocation())
                .addressDetail("101동 1001호")
                .receiverName("홍길동")
                .phone("010-1234-5678")
                .build();
        when(userDeliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));
        when(keywordMatchService.matchForGroup(
                book.getTitle(), book.getAuthor(), request.getGroupName()
        )).thenReturn(List.of());

        groupService.createGroup(host, request);

        ArgumentCaptor<GroupPlace> captor = ArgumentCaptor.forClass(GroupPlace.class);
        verify(groupPlaceRepository).save(captor.capture());
        GroupPlace snapshot = captor.getValue();
        assertThat(snapshot.getSourceType()).isEqualTo(GroupPlaceSourceType.USER_DELIVERY);
        assertThat(snapshot.getPlaceName()).isEqualTo("우리집");
        assertThat(snapshot.getAddress()).isEqualTo("서울특별시 동작구 상도로 100");
        assertThat(snapshot.getZipCode()).isEqualTo("06978");
        assertThat(snapshot.getAddressDetail()).isEqualTo("101동 1001호");
        assertThat(snapshot.getReceiverName()).isEqualTo("홍길동");
        assertThat(snapshot.getPhoneNumber()).isEqualTo("010-1234-5678");
        verifyNoInteractions(userExchangeRepository);
    }

    @Test
    void failsDeliveryGroupCreationWhenDeliveryAddressDoesNotExist() {
        when(userDeliveryRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.createGroup(host, request(TradeType.DELIVERY, 1L, null)))
                .isInstanceOfSatisfying(GroupException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(GroupErrorCode.DELIVERY_ADDRESS_NOT_FOUND));

        verifyNoInteractions(userExchangeRepository);
        verify(groupPlaceRepository, never()).save(any());
    }

    @Test
    void failsDeliveryGroupCreationWhenUserDeliveryIdIsMissing() {
        assertThatThrownBy(() -> groupService.createGroup(host, request(TradeType.DELIVERY, null, null)))
                .isInstanceOfSatisfying(GroupException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(GroupErrorCode.USER_DELIVERY_ID_REQUIRED));

        verifyNoInteractions(userExchangeRepository);
        verifyNoInteractions(userDeliveryRepository);
        verify(groupPlaceRepository, never()).save(any());
    }

    @Test
    void failsDeliveryGroupCreationWhenUserExchangeIdIsPassedInsteadOfUserDeliveryId() {
        assertThatThrownBy(() -> groupService.createGroup(host, request(TradeType.DELIVERY, null, 1L)))
                .isInstanceOfSatisfying(GroupException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(GroupErrorCode.USER_EXCHANGE_ID_NOT_ALLOWED_FOR_DELIVERY));

        verifyNoInteractions(userExchangeRepository);
        verifyNoInteractions(userDeliveryRepository);
        verify(groupPlaceRepository, never()).save(any());
    }

    @Test
    void failsDeliveryGroupCreationWhenDeliveryAddressBelongsToAnotherUser() {
        User otherUser = User.builder().id(2L).build();
        UserDelivery delivery = UserDelivery.builder()
                .id(1L)
                .user(otherUser)
                .location(deliveryLocation())
                .receiverName("홍길동")
                .phone("010-1234-5678")
                .build();
        when(userDeliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        assertThatThrownBy(() -> groupService.createGroup(host, request(TradeType.DELIVERY, 1L, null)))
                .isInstanceOfSatisfying(GroupException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo(GroupErrorCode.NOT_MY_DELIVERY_ADDRESS));
    }

    private GroupRequestDTO.CreateDTO request(TradeType tradeType, Long userDeliveryId, Long userExchangeId) {
        GroupRequestDTO.CreateDTO request = new GroupRequestDTO.CreateDTO();
        ReflectionTestUtils.setField(request, "isbn13", "9791167527721");
        ReflectionTestUtils.setField(request, "readingPeriod", 14);
        ReflectionTestUtils.setField(request, "groupComment", "숭실대 근처에서 같이 경제 서적 읽으실 분 구해요!");
        ReflectionTestUtils.setField(request, "tradeType", tradeType);
        ReflectionTestUtils.setField(request, "userDeliveryId", userDeliveryId);
        ReflectionTestUtils.setField(request, "userExchangeId", userExchangeId);
        ReflectionTestUtils.setField(request, "groupName", "같이 읽어요");
        ReflectionTestUtils.setField(request, "rules", List.of(new RuleDTO(Tag.MEMO, null)));
        return request;
    }

    private Location exchangeLocation() {
        return Location.builder()
                .placeName("숭실대입구역")
                .address("서울특별시 동작구 상도로 378")
                .zipCode("06978")
                .x(new BigDecimal("126.9534"))
                .y(new BigDecimal("37.4963"))
                .build();
    }

    private Location deliveryLocation() {
        return Location.builder()
                .placeName("우리집")
                .address("서울특별시 동작구 상도로 100")
                .zipCode("06978")
                .build();
    }
}
