package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.event.GroupMatchedEvent;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.location.service.LocationService;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.tracker.converter.TrackerConverter;
import com.example.bookiibookii.domain.tracker.dto.req.ExtendReadingPeriodReqDTO;
import com.example.bookiibookii.domain.tracker.dto.req.ReadingProgressRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerMeetingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerReceiveRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.*;
import com.example.bookiibookii.domain.tracker.dto.res.ExtendReadingPeriodResDTO;
import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.entity.TrackingImage;
import com.example.bookiibookii.domain.tracker.enums.DeliveryStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.TrackerImageException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerImageErrorCode;
import com.example.bookiibookii.domain.tracker.repository.DeliveryRepository;
import com.example.bookiibookii.domain.tracker.repository.TrackingImageRepository;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import com.example.bookiibookii.domain.tracker.resolver.*;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.groupbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.groupbook.repository.GroupBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.example.bookiibookii.domain.tracker.enums.TrackerAction.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackerService {

    private final TrackerRepository trackerRepository;
    private final DeliveryRepository deliveryRepository;
    private final TrackingImageRepository trackingImageRepository;
    private final TrackerImageValidationService trackerImageValidationService;
    private final TrackerImageS3Service trackerImageS3Service;
    private final MatchedMemberRepository matchedMemberRepository;
    private final GroupBookRepository groupBookRepository;
    private final GroupsRepository groupsRepository;
    private final MeetingRepository meetingRepository;
    private final TrackerConverter trackerConverter;
    private final TrackerPartnerResolver trackerPartnerResolver;
    private final TrackerDisplayStatusResolver trackerDisplayStatusResolver;
    private final TrackerDueDateResolver trackerDueDateResolver;
    private final UserProfileImageUrlResolver userProfileImageUrlResolver;
    private final TrackerStepAssembler trackerStepAssembler;

    private static final int TRACKER_IMAGE_PRESIGNED_URL_EXPIRATION_MINUTES = 10;
    private static final int TRACKER_IMAGE_GET_URL_EXPIRATION_MINUTES = 60;

    // 트래커 리스트 조회
    public List<TrackerListItemResDTO> getTrackerList(User user) {
        return matchedMemberRepository.findAllTrackerItemsByMemberId(user.getId()).stream()
                .map(me -> {
                    MatchedMember partner = trackerPartnerResolver.resolve(me.getGroup(), me);
                    if (me.getCurrentMemberBook() == null || partner.getCurrentMemberBook() == null) {
                        throw new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND);
                    }

                    TrackerDisplayStatus displayStatus = trackerDisplayStatusResolver.resolve(
                            me.getReadingStatus(),
                            me.getExchangeStatus(),
                            me.getGroup().getTradeType()
                    );

                    return TrackerConverter.toListItem(
                            me,
                            partner,
                            displayStatus,
                            trackerDueDateResolver.calculate(displayStatus, me.getGroup()),
                            userProfileImageUrlResolver.resolve(me.getCurrentMemberBook().getMatchedMember().getUser()),
                            userProfileImageUrlResolver.resolve(partner.getCurrentMemberBook().getMatchedMember().getUser())
                    );
                })
                .toList();
    }

    // 교환독서 상세조회
    @Transactional(readOnly = true)
    public TrackerDetailResDTO getTrackerDetail(Long groupId, User user) {
        List<MatchedMember> matchedMembers = matchedMemberRepository.findAllTrackerMembersByGroupId(groupId);

        MatchedMember me = matchedMembers.stream()
                .filter(matchedMember -> matchedMember.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember partner = matchedMembers.stream()
                .filter(matchedMember -> !matchedMember.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.PARTNER_NOT_FOUND));

        if (me.getCurrentMemberBook() == null || partner.getCurrentMemberBook() == null) {
            throw new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND);
        }

        TrackerDisplayStatus displayStatus = trackerDisplayStatusResolver.resolve(
                me.getReadingStatus(),
                me.getExchangeStatus(),
                me.getGroup().getTradeType()
        );

        return TrackerConverter.toDetail(
                me,
                partner,
                displayStatus,
                trackerDueDateResolver.calculate(displayStatus, me.getGroup()),
                userProfileImageUrlResolver.resolve(me.getCurrentMemberBook().getMatchedMember().getUser()),
                userProfileImageUrlResolver.resolve(partner.getCurrentMemberBook().getMatchedMember().getUser()),
                trackerStepAssembler.assemble(me)
        );
    }

    @Transactional
    public ReadingProgressResponseDTO updateReadingProgress(Long groupId, ReadingProgressRequestDTO request, User user) {
        if (request == null || request.currentPage() == null || request.currentPage() < 0) {
            throw new TrackerException(TrackerErrorCode.INVALID_READING_PROGRESS);
        }

        MatchedMember me = getMyMatchedMember(groupId, user.getId());
        if (me.getCurrentMemberBook() == null || me.getCurrentMemberBook().getBook() == null) {
            throw new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND);
        }

        Integer totalPages = me.getCurrentMemberBook().getBook().getTotalPages();
        if (totalPages == null || totalPages <= 0) {
            throw new TrackerException(TrackerErrorCode.INVALID_BOOK_TOTAL_PAGES);
        }

        if (request.currentPage() > totalPages) {
            throw new TrackerException(TrackerErrorCode.INVALID_READING_PROGRESS);
        }

        int normalizedPage = request.currentPage();
        ReadingStatus currentStatus = me.getReadingStatus();
        validateReadingProgressStatus(currentStatus, normalizedPage, totalPages);

        me.getCurrentMemberBook().updateCurrentPage(normalizedPage);

        if (normalizedPage == totalPages) {
            if (currentStatus == ReadingStatus.MY_BOOK_READING) {
                me.updateReadingStatus(ReadingStatus.MY_BOOK_REVIEWING);
            } else if (currentStatus == ReadingStatus.PARTNER_BOOK_READING) {
                me.updateReadingStatus(ReadingStatus.PARTNER_BOOK_REVIEWING);
            }
        }

        ReadingStatus updatedStatus = me.getReadingStatus();
        return ReadingProgressResponseDTO.builder()
                .memberBookId(me.getCurrentMemberBook().getId())
                .currentPage(me.getCurrentMemberBook().getCurrentPage())
                .totalPages(totalPages)
                .progressRate(calculateProgressRate(me.getCurrentMemberBook().getCurrentPage(), totalPages))
                .readingStatus(updatedStatus)
                .readingStatusText(updatedStatus.getDescription())
                .dDay(calculateReadingDDay(updatedStatus, me.getGroup()))
                .build();
    }

    // --- 이미지 관련 ---

    public PresignedUrlResponseDTO getPresignedPutUrlForTrackerImage(Long groupId, User user) {
        validateGroupMember(groupId, user.getId());
        return trackerImageS3Service.generatePresignedPutUrl(TRACKER_IMAGE_PRESIGNED_URL_EXPIRATION_MINUTES);
    }

    public TrackerImageGetResponseDTO getShippingProofImageUrl(Long groupId, User user) {
        validateGroupMember(groupId, user.getId());
        MatchedMember me = getMyMatchedMember(groupId, user.getId());

        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        Delivery shippingDelivery = deliveryRepository
                .findTopByTrackerAndReceiverIdAndDeliveryStatusOrderByCreatedAtDesc(tracker, me.getId(), DeliveryStatus.SHIPPING)
                .orElseThrow(() -> new TrackerImageException(TrackerImageErrorCode.TRACKING_IMAGE_NOT_FOUND));

        TrackingImage image = trackingImageRepository
                .findTopByDelivery_IdOrderByCreatedAtAsc(shippingDelivery.getId())
                .orElseThrow(() -> new TrackerImageException(TrackerImageErrorCode.TRACKING_IMAGE_NOT_FOUND));

        String presignedGetUrl = trackerImageS3Service.generatePresignedGetUrl(image.getS3Key(), TRACKER_IMAGE_GET_URL_EXPIRATION_MINUTES);
        return TrackerImageGetResponseDTO.builder().presignedGetUrl(presignedGetUrl).build();
    }

    // --- 트래커 생성 ---

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTracker(GroupMatchedEvent event) {
        if (trackerRepository.existsByGroup_GroupId(event.groupId())) {
            throw new TrackerException(TrackerErrorCode.TRACKER_ALREADY_EXISTS);
        }

        Groups group = groupsRepository.findById(event.groupId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        Tracker tracker = Tracker.builder()
                .group(group)
                .readingStatus(ReadingStatus.MY_BOOK_READING)
                .startDate(group.getStartDate().atStartOfDay())
                .endDate(group.getStartDate().atStartOfDay().plusDays(group.getReadingPeriod()))
                .extensionCount(0)
                .extensionDays(0)
                .build();

        trackerRepository.save(tracker);

        List<GroupBook> groupBooks = groupBookRepository.findAllByGroup_GroupId(event.groupId());
        if (!groupBooks.isEmpty()) {
            groupBooks.forEach(ub -> ub.assignTracker(tracker));
        }
    }

    @Transactional
    public ExtendReadingPeriodResDTO extendReadingPeriod(Long groupId, ExtendReadingPeriodReqDTO request, User user) {
        RoleStatus role = matchedMemberRepository.findRoleByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));
        if (role != RoleStatus.HOST) {
            throw new TrackerException(TrackerErrorCode.NOT_GROUP_HOST);
        }

        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        tracker.extendReadingPeriod(request.newEndDate());

        Groups group = tracker.getGroup();
        long newPeriod = ChronoUnit.DAYS.between(group.getStartDate(), request.newEndDate());
        group.setReadingPeriod((int) newPeriod);

        int dDay = (int) ChronoUnit.DAYS.between(LocalDate.now(), request.newEndDate());
        return new ExtendReadingPeriodResDTO(request.newEndDate(), dDay);
    }

    // --- 트래커 조회 ---



    private ReadingStatus resolveMeetingStatus(ReadingStatus current) {
        return switch (current) {
            case MY_BOOK_REVIEWING, EXCHANGING -> ReadingStatus.EXCHANGING;
            case PARTNER_BOOK_REVIEWING, RETURNING -> ReadingStatus.RETURNING;
            default -> null;
        };
    }

    private MatchedMember findPartnerForRelay(Long groupId, Long myUserId) {
        List<MatchedMember> members = matchedMemberRepository.findAllByGroup_GroupId(groupId);
        if (members.size() > 2) {
            throw new TrackerException(TrackerErrorCode.INVALID_PARTNER_COUNT);
        }
        return members.stream()
                .filter(mm -> !mm.getUser().getId().equals(myUserId))
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.PARTNER_NOT_FOUND));
    }

    private String findTargetNickName(Tracker tracker, Long userId) {
        return tracker.getGroup().getMatchedMember().stream()
                .filter(mm -> mm.getUser() != null && !mm.getUser().getId().equals(userId))
                .map(mm -> {
                    String nickname = mm.getUser().getNickName();
                    return nickname != null ? nickname : "닉네임 미설정";
                })
                .findFirst()
                .orElse("상대방 없음");
    }

    private List<String> buildStepDates(Tracker tracker) {
        List<String> dates = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM. dd.");

        if (tracker.getStartDate() != null) {
            dates.add(tracker.getStartDate().format(formatter));
        }

        List<Delivery> deliveries = tracker.getDeliveries();
        if (deliveries == null || deliveries.isEmpty()) return dates;

        List<Delivery> shippingDeliveries = deliveries.stream()
                .filter(d -> d.getDeliveryStatus() == DeliveryStatus.SHIPPING ||
                             d.getDeliveryStatus() == DeliveryStatus.RETURNED)
                .sorted(Comparator.comparing(Delivery::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());

        // 1차 교환 시작일
        if (!shippingDeliveries.isEmpty() && shippingDeliveries.get(0).getStartDate() != null) {
            dates.add(shippingDeliveries.get(0).getStartDate().format(formatter));
        }

        // 2차 교환(반납) 시작일 — 교환 단계 2건 이후부터 반납 단계
        if (shippingDeliveries.size() > 2 && shippingDeliveries.get(2).getStartDate() != null) {
            dates.add(shippingDeliveries.get(2).getStartDate().format(formatter));
        }

        return dates;
    }

    private void validateReadingProgressStatus(ReadingStatus status, int normalizedPage, int totalPages) {
        if (status == ReadingStatus.MY_BOOK_READING || status == ReadingStatus.PARTNER_BOOK_READING) {
            return;
        }

        if ((status == ReadingStatus.MY_BOOK_REVIEWING || status == ReadingStatus.PARTNER_BOOK_REVIEWING)
                && normalizedPage == totalPages) {
            return;
        }

        throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
    }

    private int calculateProgressRate(Integer currentPage, Integer totalPages) {
        if (currentPage == null || totalPages == null || totalPages <= 0) {
            return 0;
        }
        int normalizedPage = Math.min(Math.max(currentPage, 0), totalPages);
        return (normalizedPage * 100) / totalPages;
    }

    private Integer calculateReadingDDay(ReadingStatus readingStatus, Groups group) {
        if (readingStatus != ReadingStatus.MY_BOOK_READING
                && readingStatus != ReadingStatus.PARTNER_BOOK_READING) {
            return null;
        }
        if (group.getStartDate() == null || group.getReadingPeriod() == null) {
            return null;
        }

        LocalDate dueDate = group.getStartDate().plusDays(group.getReadingPeriod());
        return Math.max((int) ChronoUnit.DAYS.between(LocalDate.now(), dueDate), 0);
    }

    // --- 택배 배송/수령 (PARCEL) ---
    /*
    @Transactional
    public void registerShipping(Long groupId, TrackerShippingRequestDTO request, User user) {
        Tracker tracker = trackerRepository.findByGroupIdForUpdate(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember me = getMyMatchedMember(groupId, user.getId());
        MatchedMember partner = getPartnerMember(groupId, me.getId());
        ReadingStatus status = tracker.getReadingStatus();

        if (status == ReadingStatus.MY_BOOK_REVIEWING || status == ReadingStatus.EXCHANGING) {
            if (me.getReadingStatus() != ReadingStatus.MY_BOOK_REVIEW_DONE) {
                throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
            }
            if (deliveryRepository.existsByTrackerAndSenderIdAndDeliveryStatus(tracker, me.getId(), DeliveryStatus.SHIPPING)) {
                throw new TrackerException(TrackerErrorCode.ALREADY_SHIPPED);
            }
            if (status == ReadingStatus.MY_BOOK_REVIEWING) {
                tracker.startExchanging(); // MY_BOOK_REVIEWING → EXCHANGING
            }
        } else if (status == ReadingStatus.PARTNER_BOOK_REVIEWING || status == ReadingStatus.RETURNING) {
            if (me.getReadingStatus() != ReadingStatus.PARTNER_BOOK_REVIEW_DONE) {
                throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
            }
            if (deliveryRepository.existsByTrackerAndSenderIdAndDeliveryStatus(tracker, me.getId(), DeliveryStatus.SHIPPING)) {
                throw new TrackerException(TrackerErrorCode.ALREADY_SHIPPED);
            }
            if (status == ReadingStatus.PARTNER_BOOK_REVIEWING) {
                tracker.startReturning(); // PARTNER_BOOK_REVIEWING → RETURNING
            }
        } else {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        validateTrackerImageS3Key(request.s3Key());

        Delivery shippingDelivery = Delivery.builder()
                .id(UUID.randomUUID().toString())
                .tracker(tracker)
                .deliveryStatus(DeliveryStatus.SHIPPING)
                .sender(me)
                .receiver(partner)
                .deliveryCompany(request.deliveryCompany())
                .trackingNumber(request.trackingNumber())
                .startDate(LocalDateTime.now())
                .build();
        deliveryRepository.save(shippingDelivery);

        trackingImageRepository.save(TrackingImage.builder()
                .delivery(shippingDelivery)
                .s3Key(request.s3Key())
                .build());

        publisher.publish(new TrackerNotificationEvent(SHIPPING_REGISTERED, user.getId(), groupId, null));
    }

    @Transactional
    public void registerReceive(Long groupId, TrackerReceiveRequestDTO request, User user) {
        Tracker tracker = trackerRepository.findByGroupIdForUpdate(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember me = getMyMatchedMember(groupId, user.getId());
        ReadingStatus status = tracker.getReadingStatus();

        if (status != ReadingStatus.EXCHANGING && status != ReadingStatus.RETURNING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        Delivery shippingDelivery = deliveryRepository
                .findTopByTrackerAndReceiverIdAndDeliveryStatusOrderByCreatedAtDesc(tracker, me.getId(), DeliveryStatus.SHIPPING)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS));

        validateTrackerImageS3Key(request.s3Key());

        shippingDelivery.complete(LocalDateTime.now()); // SHIPPING → RETURNED

        trackingImageRepository.save(TrackingImage.builder()
                .delivery(shippingDelivery)
                .s3Key(request.s3Key())
                .build());

        // 남은 SHIPPING 배송이 없으면 → 양측 수령 완료 → 다음 단계 진행
        if (!deliveryRepository.existsByTrackerAndDeliveryStatus(tracker, DeliveryStatus.SHIPPING)) {
            if (status == ReadingStatus.EXCHANGING) {
                tracker.completeExchange(); // EXCHANGING → EXCHANGED
            } else {
                me.updateReadingStatus(ReadingStatus.DONE);
                getPartnerMember(groupId, me.getId()).updateReadingStatus(ReadingStatus.DONE);
                tracker.completeRelay(); // RETURNING → COMPLETED
            }
        }

        publisher.publish(new TrackerNotificationEvent(RECEIVED_CONFIRMED, user.getId(), groupId, null));
    }

    // --- 직접 교환(Meeting, DIRECT) ---

    public TrackerMeetingResponseDTO getMeetingDetailByGroupId(Long groupId, User user) {
        validateGroupMember(groupId, user.getId());

        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        if (tracker.getGroup().getTradeType() != TradeType.DIRECT) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRADE_TYPE);
        }

        ReadingStatus meetingStatus = resolveMeetingStatus(tracker.getReadingStatus());
        if (meetingStatus == null) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        return meetingRepository.findByTrackerIdAndStatusNative(tracker.getId(), meetingStatus.name())
                .map(meeting -> {
                    Location loc = meeting.getLocation();
                    return new TrackerMeetingResponseDTO(
                            meeting.getMeetingTime(),
                            loc != null ? loc.getPlaceName() : tracker.getGroup().getPreferRegion(),
                            loc != null ? loc.getAddress() : null
                    );
                })
                .orElseGet(() -> new TrackerMeetingResponseDTO(null, tracker.getGroup().getPreferRegion(), null));
    }

    @Transactional
    public void updateMeeting(Long groupId, TrackerMeetingRequestDTO request, User user) {
        validateGroupMember(groupId, user.getId());

        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        if (tracker.getGroup().getTradeType() != TradeType.DIRECT) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRADE_TYPE);
        }

        ReadingStatus currentStatus = tracker.getReadingStatus();
        ReadingStatus meetingPhaseStatus = resolveMeetingStatus(currentStatus);
        if (meetingPhaseStatus == null) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        Meeting meeting = meetingRepository.findByTrackerIdAndStatusNative(tracker.getId(), meetingPhaseStatus.name())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.MEETING_NOT_FOUND));

        Location location = locationService.findOrCreate(request.placeName(), request.address(), request.zipCode());

        if (meeting.getMeetingTime() != null) {
            meeting.resetConfirmation();
        }
        meeting.setMeetingDetails(location, request.meetingTime());

        // 첫 번째로 미팅을 등록하면 교환/반납 단계로 진입
        if (currentStatus == ReadingStatus.MY_BOOK_REVIEWING) {
            tracker.startExchanging(); // MY_BOOK_REVIEWING → EXCHANGING
        } else if (currentStatus == ReadingStatus.PARTNER_BOOK_REVIEWING) {
            tracker.startReturning(); // PARTNER_BOOK_REVIEWING → RETURNING
        }
    }

    @Transactional
    public void completeMeeting(Long groupId, User user) {
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        if (tracker.getGroup().getTradeType() != TradeType.DIRECT) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRADE_TYPE);
        }

        ReadingStatus currentStatus = tracker.getReadingStatus();
        if (currentStatus != ReadingStatus.EXCHANGING && currentStatus != ReadingStatus.RETURNING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        RoleStatus userRole = matchedMemberRepository.findRoleByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));

        Meeting meeting = meetingRepository.findByTrackerWithLock(tracker.getId(), currentStatus)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.MEETING_NOT_FOUND));

        meeting.confirm(userRole);

        if (meeting.isFullyConfirmed()) {
            if (currentStatus == ReadingStatus.EXCHANGING) {
                tracker.completeExchange(); // EXCHANGING → EXCHANGED
            } else {
                // 반납 완료 시 양측 ReadingStatus → DONE
                MatchedMember me = getMyMatchedMember(groupId, user.getId());
                MatchedMember partner = getPartnerMember(groupId, me.getId());
                me.updateReadingStatus(ReadingStatus.DONE);
                partner.updateReadingStatus(ReadingStatus.DONE);
                tracker.completeRelay(); // RETURNING → COMPLETED
            }
            publisher.publish(new TrackerNotificationEvent(RECEIVED_CONFIRMED, user.getId(), groupId, null));
        } else {
            log.info("상대방 확인 대기 중.");
        }
    } */

    // --- 내부 유틸 ---
    private void validateTrackerImageS3Key(String s3Key) {
        if (!trackerImageValidationService.isValidS3Key(s3Key)) {
            throw new TrackerImageException(TrackerImageErrorCode.INVALID_S3_KEY_FORMAT);
        }
        if (!trackerImageS3Service.doesImageExist(s3Key)) {
            throw new TrackerImageException(TrackerImageErrorCode.IMAGE_NOT_FOUND_IN_S3);
        }
        if (trackingImageRepository.existsByS3Key(s3Key)) {
            throw new TrackerImageException(TrackerImageErrorCode.DUPLICATE_S3_KEY);
        }
    }

    // --- Helpers ---
    private void validateGroupMember(Long groupId, Long userId) {
        if (!matchedMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, userId)) {
            throw new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER);
        }
    }

    private MatchedMember getMyMatchedMember(Long groupId, Long userId) {
        return matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));
    }

    private MatchedMember getPartnerMember(Long groupId, Long myMemberId) {
        return matchedMemberRepository.findAllByGroup_GroupId(groupId).stream()
                .filter(mm -> !mm.getId().equals(myMemberId))
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.PARTNER_NOT_FOUND));
    }
}
