package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.service.BookService;
import com.example.bookiibookii.domain.group.dto.req.ApplicationRequestDTO;
import com.example.bookiibookii.domain.group.entity.Application;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.event.GroupNotificationEvent;
import com.example.bookiibookii.domain.group.repository.ApplicationRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.service.MatchedMemberCardStateCleanupService;
import com.example.bookiibookii.domain.memberbook.service.MemberBookService;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.time.Clock;

import static com.example.bookiibookii.domain.group.enums.GroupNotiType.JOIN_REQUESTED;
import static com.example.bookiibookii.domain.group.enums.GroupNotiType.MATCH_REJECTED;
import static com.example.bookiibookii.domain.group.enums.GroupNotiType.MATCH_SUCCEEDED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationNotificationEventTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private GroupsRepository groupsRepository;
    @Mock private UserRepository userRepository;
    @Mock private MatchedMemberRepository matchedMemberRepository;
    @Mock private DomainEventPublisher publisher;
    @Mock private MemberBookService memberBookService;
    @Mock private MatchedMemberCardStateCleanupService matchedMemberCardStateCleanupService;
    @Mock private UserImageS3Service userImageS3Service;
    @Mock private BookService bookService;

    private ApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationService(
                applicationRepository,
                groupsRepository,
                userRepository,
                matchedMemberRepository,
                publisher,
                memberBookService,
                matchedMemberCardStateCleanupService,
                userImageS3Service,
                bookService,
                Clock.systemUTC()
        );
    }

    @Test
    void joinRequestEventContainsPersistedRequestId() {
        User host = user(1L, "호스트");
        User guest = user(2L, "게스트");
        Book book = Book.builder().id(1L).title("책").build();
        Groups group = group(host, book);
        when(groupsRepository.findByIdForUpdateWithBookAndHost(group.getId())).thenReturn(Optional.of(group));
        when(userRepository.findById(guest.getId())).thenReturn(Optional.of(guest));
        when(bookService.getOrCreateByIsbn13("1234567890123")).thenReturn(book);
        when(applicationRepository.save(any(Application.class))).thenAnswer(invocation -> {
            Application application = invocation.getArgument(0);
            ReflectionTestUtils.setField(application, "applicationId", 100L);
            return application;
        });

        ApplicationRequestDTO.JoinApplicationDTO request = new ApplicationRequestDTO.JoinApplicationDTO();
        ReflectionTestUtils.setField(request, "isbn13", "1234567890123");
        service.joinGroup(group.getId(), guest.getId(), request);

        GroupNotificationEvent event = captureEvent();
        assertThat(event.type()).isEqualTo(JOIN_REQUESTED);
        assertThat(event.requestId()).isEqualTo(100L);
        assertThat(event.receiverId()).isEqualTo(host.getId());
    }

    @Test
    void acceptedRequestEventContainsRequestIdAndExchangeType() {
        Application application = application(100L);
        Groups group = application.getGroup();
        when(applicationRepository.findById(application.getApplicationId())).thenReturn(Optional.of(application));
        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(groupsRepository.findByIdWithBookAndHost(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.countByGroup(group)).thenReturn(1L);
        when(applicationRepository.findAllPendingByGroupId(group.getId())).thenReturn(List.of());

        service.updateApplicationStatus(
                application.getApplicationId(), group.getHost().getId(), ApplicationStatus.ACCEPTED);

        GroupNotificationEvent event = captureEvent();
        assertThat(event.type()).isEqualTo(MATCH_SUCCEEDED);
        assertThat(event.requestId()).isEqualTo(application.getApplicationId());
        assertThat(event.exchangeType()).isNotNull();
    }

    @Test
    void rejectedRequestEventContainsRequestId() {
        Application application = application(100L);
        Groups group = application.getGroup();
        when(applicationRepository.findById(application.getApplicationId())).thenReturn(Optional.of(application));
        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(groupsRepository.findByIdWithBookAndHost(group.getId())).thenReturn(Optional.of(group));

        service.updateApplicationStatus(
                application.getApplicationId(), group.getHost().getId(), ApplicationStatus.REJECTED);

        GroupNotificationEvent event = captureEvent();
        assertThat(event.type()).isEqualTo(MATCH_REJECTED);
        assertThat(event.requestId()).isEqualTo(application.getApplicationId());
        assertThat(event.receiverId()).isEqualTo(application.getGuest().getId());
    }

    private GroupNotificationEvent captureEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publish(captor.capture());
        return (GroupNotificationEvent) captor.getValue();
    }

    private Application application(Long id) {
        User host = user(1L, "호스트");
        User guest = user(2L, "게스트");
        Book book = Book.builder().id(1L).title("책").build();
        return Application.builder()
                .applicationId(id)
                .group(group(host, book))
                .guest(guest)
                .book(book)
                .applicationStatus(ApplicationStatus.PENDING)
                .build();
    }

    private Groups group(User host, Book book) {
        return Groups.builder()
                .id(10L)
                .host(host)
                .book(book)
                .groupName("교환 독서")
                .maxCapacity(2)
                .tradeType(TradeType.DELIVERY)
                .groupStatus(GroupStatus.RECRUITING)
                .build();
    }

    private User user(Long id, String nickname) {
        return User.builder().id(id).nickName(nickname).build();
    }
}
