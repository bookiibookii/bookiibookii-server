package com.example.bookiibookii.domain.notification.repository;

import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.service.NotificationPersistenceService;
import com.example.bookiibookii.domain.notification.service.NotificationStore;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NotificationPersistenceService.class, NotificationStore.class})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationDeduplicationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationStore notificationStore;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void sameReceiverAndDedupKeyIsStoredOnce() {
        User receiver = saveUser("receiver-1");

        assertThat(notificationStore.save(notification(receiver.getId(), "LIB:1:2"))).isPresent();
        assertThat(notificationStore.save(notification(receiver.getId(), "LIB:1:2"))).isEmpty();

        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    @Test
    void nullDedupKeyAllowsMultipleNotifications() {
        User receiver = saveUser("receiver-2");

        assertThat(notificationStore.save(notification(receiver.getId(), null))).isPresent();
        assertThat(notificationStore.save(notification(receiver.getId(), null))).isPresent();

        assertThat(notificationRepository.count()).isEqualTo(2);
    }

    @Test
    void sameDedupKeyCanBeStoredForDifferentReceivers() {
        User firstReceiver = saveUser("receiver-3");
        User secondReceiver = saveUser("receiver-4");

        assertThat(notificationStore.save(notification(firstReceiver.getId(), "OPS:1"))).isPresent();
        assertThat(notificationStore.save(notification(secondReceiver.getId(), "OPS:1"))).isPresent();

        assertThat(notificationRepository.count()).isEqualTo(2);
    }

    @Test
    void trackerBookReviewCompletionIsDeduplicatedPerActorAndExchangeRound() {
        User host = saveUser("host");
        User guest = saveUser("guest");

        String hostFirstRound = "TRACKER_READING_REVIEW_COMPLETED:%d:10:%d:100:FIRST_EXCHANGE"
                .formatted(guest.getId(), host.getId());
        String hostSecondRound = "TRACKER_READING_REVIEW_COMPLETED:%d:10:%d:100:RETURN_EXCHANGE"
                .formatted(guest.getId(), host.getId());
        String guestFirstRound = "TRACKER_READING_REVIEW_COMPLETED:%d:10:%d:200:FIRST_EXCHANGE"
                .formatted(host.getId(), guest.getId());

        assertThat(notificationStore.save(notification(guest.getId(), hostFirstRound))).isPresent();
        assertThat(notificationStore.save(notification(guest.getId(), hostFirstRound))).isEmpty();
        assertThat(notificationStore.save(notification(guest.getId(), hostSecondRound))).isPresent();
        assertThat(notificationStore.save(notification(host.getId(), guestFirstRound))).isPresent();

        assertThat(notificationRepository.count()).isEqualTo(3);
    }

    @Test
    void directMeetingChangeDuplicateIsIgnoredWithoutFailingCaller() {
        User receiver = saveUser("direct-receiver");
        String key = "DIRECT_MEETING_UPDATED:group:10:round:FIRST_EXCHANGE:receiver:%d:"
                .formatted(receiver.getId())
                + "meeting:20:event:550e8400-e29b-41d4-a716-446655440000";

        assertThat(notificationStore.save(notification(receiver.getId(), key))).isPresent();
        assertThat(notificationStore.save(notification(receiver.getId(), key))).isEmpty();
        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    @Test
    void directReminderAllowsNewMeetingTimeButDeduplicatesSameTime() {
        User receiver = saveUser("reminder-receiver");
        String first = "DIRECT_MEETING_CONFIRM_REMINDER:group:10:round:FIRST_EXCHANGE:receiver:%d:"
                .formatted(receiver.getId())
                + "meetingAt:2026-06-20T14:00";
        String changed = "DIRECT_MEETING_CONFIRM_REMINDER:group:10:round:FIRST_EXCHANGE:receiver:%d:"
                .formatted(receiver.getId())
                + "meetingAt:2026-06-21T14:00";

        assertThat(notificationStore.save(notification(receiver.getId(), first))).isPresent();
        assertThat(notificationStore.save(notification(receiver.getId(), first))).isEmpty();
        assertThat(notificationStore.save(notification(receiver.getId(), changed))).isPresent();
        assertThat(notificationRepository.count()).isEqualTo(2);
    }

    @Test
    void readingCardReactionStaysDeduplicatedAfterReactionChangesOrReaddition() {
        User receiver = saveUser("card-owner");
        String key = "READING_CARD_REACTION_CREATED:card:20:reactor:30";

        assertThat(notificationStore.save(notification(receiver.getId(), key))).isPresent();
        assertThat(notificationStore.save(notification(receiver.getId(), key))).isEmpty();
        assertThat(notificationStore.save(notification(receiver.getId(), key))).isEmpty();
        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    @Test
    void nonDedupIntegrityViolationIsNotIgnored() {
        User receiver = saveUser("receiver-5");
        Notification invalid = Notification.builder()
                .receiver(receiver)
                .category(NotificationCategory.SYSTEM)
                .type(NotificationType.GROUP_COMMENT_CREATED)
                .title(null)
                .message("message")
                .payload("{}")
                .dedupKey("INVALID:1")
                .read(false)
                .build();

        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> notificationStore.save(invalid)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private User saveUser(String socialId) {
        return userRepository.saveAndFlush(
                User.builder()
                        .socialType(SocialType.KAKAO)
                        .socialId(socialId)
                        .build()
        );
    }

    private Notification notification(Long receiverId, String dedupKey) {
        NotificationFactory factory = new NotificationFactory(userRepository, new ObjectMapper());
        return factory.create(
                receiverId,
                NotificationCategory.SYSTEM,
                NotificationType.GROUP_COMMENT_CREATED,
                "title",
                "message",
                "{}",
                dedupKey
        );
    }
}
