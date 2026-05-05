package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.Location;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.event.GroupMatchedEvent;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.LocationRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.tracker.converter.TrackerConverter;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerMeetingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerReceiveRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerImageGetResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerMeetingResponseDTO;
import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.entity.TrackingImage;
import com.example.bookiibookii.domain.tracker.enums.DeliveryStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.TrackerImageException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerImageErrorCode;
import com.example.bookiibookii.domain.tracker.repository.DeliveryRepository;
import com.example.bookiibookii.domain.tracker.repository.TrackingImageRepository;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private final UserBookRepository userBookRepository;
    private final GroupsRepository groupsRepository;
    private final MeetingRepository meetingRepository;
    private final LocationRepository locationRepository;
    private final TrackerConverter trackerConverter;
    private final DomainEventPublisher publisher;

    private static final int TRACKER_IMAGE_PRESIGNED_URL_EXPIRATION_MINUTES = 10;
    private static final int TRACKER_IMAGE_GET_URL_EXPIRATION_MINUTES = 60;

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

    public TrackerImageGetResponseDTO getReceivedProofImageUrl(Long groupId, User user) {
        validateGroupMember(groupId, user.getId());
        MatchedMember me = getMyMatchedMember(groupId, user.getId());

        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        // 내가 보낸 배송이 RETURNED 되었을 때 수령인이 올린 이미지(최신)를 반환
        Delivery myReturnedDelivery = deliveryRepository
                .findTopByTrackerAndSenderIdAndDeliveryStatusOrderByCreatedAtDesc(tracker, me.getId(), DeliveryStatus.RETURNED)
                .orElseThrow(() -> new TrackerImageException(TrackerImageErrorCode.RECEIVED_IMAGE_NOT_FOUND));

        TrackingImage image = trackingImageRepository
                .findTopByDelivery_IdOrderByCreatedAtDesc(myReturnedDelivery.getId())
                .orElseThrow(() -> new TrackerImageException(TrackerImageErrorCode.RECEIVED_IMAGE_NOT_FOUND));

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
                .trackerStatus(TrackerStatus.READY)
                .startDate(group.getStartDate().atStartOfDay())
                .endDate(group.getStartDate().atStartOfDay().plusDays(group.getReadingPeriod()))
                .extensionCount(0)
                .extensionDays(0)
                .build();

        trackerRepository.save(tracker);

        // DIRECT 거래 그룹의 경우 교환/반납 단계 미팅 레코드 사전 생성
        if (group.getTradeType() == TradeType.DIRECT) {
            Location defaultLocation = Location.builder()
                    .id(UUID.randomUUID().toString())
                    .placeName(group.getPreferRegion())
                    .build();
            locationRepository.save(defaultLocation);

            meetingRepository.save(Meeting.builder()
                    .tracker(tracker)
                    .location(defaultLocation)
                    .build());
            meetingRepository.save(Meeting.builder()
                    .tracker(tracker)
                    .location(defaultLocation)
                    .build());
        }

        List<UserBook> userBooks = userBookRepository.findAllByGroup_GroupId(event.groupId());
        if (!userBooks.isEmpty()) {
            userBooks.forEach(ub -> ub.assignTracker(tracker));
        }
    }

    // --- 트래커 조회 ---

    @Transactional(readOnly = true)
    public TrackerDetailResponseDTO getTrackerDetailByGroupId(Long groupId, User user) {
        validateGroupMember(groupId, user.getId());
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember partnerMember = findPartnerForRelay(groupId, user.getId());

        Meeting latestMeeting = null;
        if (tracker.getGroup().getTradeType() == TradeType.DIRECT) {
            TrackerStatus meetingStatus = resolveMeetingStatus(tracker.getTrackerStatus());
            if (meetingStatus != null) {
                latestMeeting = meetingRepository.findByTrackerIdAndStatusNative(
                        tracker.getId(), meetingStatus.name()).orElse(null);
            }
        }

        Delivery latestShipping = deliveryRepository
                .findTopByTrackerAndDeliveryStatusOrderByCreatedAtDesc(tracker, DeliveryStatus.SHIPPING)
                .orElse(null);

        return trackerConverter.toDetailResponse(tracker, latestMeeting, latestShipping, partnerMember.getUser());
    }

    private TrackerStatus resolveMeetingStatus(TrackerStatus current) {
        return switch (current) {
            case READ_DONE, EXCHANGING -> TrackerStatus.EXCHANGING;
            case READ_DONE_2, RETURNING -> TrackerStatus.RETURNING;
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

    @Transactional(readOnly = true)
    public List<TrackerListResponseDTO> getTrackerList(Long userId) {
        return convertToResponseList(trackerRepository.findAllByUserIdWithDetails(userId), userId);
    }

    @Transactional(readOnly = true)
    public List<TrackerListResponseDTO> getHostTrackerList(Long userId) {
        return convertToResponseList(trackerRepository.findAllByUserIdAndRoleWithDetails(userId, RoleStatus.HOST), userId);
    }

    @Transactional(readOnly = true)
    public List<TrackerListResponseDTO> getGuestTrackerList(Long userId) {
        return convertToResponseList(trackerRepository.findAllByUserIdAndRoleWithDetails(userId, RoleStatus.GUEST), userId);
    }

    private List<TrackerListResponseDTO> convertToResponseList(List<Tracker> trackers, Long userId) {
        return trackers.stream()
                .map(tracker -> {
                    Groups group = tracker.getGroup();
                    List<String> stepDates = buildStepDates(tracker);
                    String targetNickname = findTargetNickName(tracker, userId);

                    Integer myRate = 0;
                    Integer groupRate = 0;

                    if (group.getGroupType() == GroupType.TOGETHER) {
                        myRate = calculateUserReadingRate(userId, group);
                        groupRate = calculateGroupAverageRate(group);
                    }

                    return trackerConverter.toListResponse(tracker, group, targetNickname, stepDates, myRate, groupRate);
                })
                .collect(Collectors.toList());
    }

    private int calculateUserReadingRate(Long userId, Groups group) {
        return group.getMatchedMember().stream()
                .filter(mm -> mm.getUser().getId().equals(userId))
                .findFirst()
                .map(mm -> mm.getCurrentReadingRate() != null ? mm.getCurrentReadingRate() : 0)
                .orElse(0);
    }

    private int calculateGroupAverageRate(Groups group) {
        List<MatchedMember> members = group.getMatchedMember();
        if (members == null || members.isEmpty()) return 0;
        double totalSum = members.stream()
                .mapToDouble(mm -> mm.getCurrentReadingRate() != null ? mm.getCurrentReadingRate() : 0)
                .sum();
        return (int) (totalSum / members.size());
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

    // --- 독서 단계 ---

    @Transactional
    public void registerReading(Long groupId, User user) {
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember me = getMyMatchedMember(groupId, user.getId());
        TrackerStatus status = tracker.getTrackerStatus();

        if (status == TrackerStatus.READY || status == TrackerStatus.READING) {
            if (me.getReadingStatus() != ReadingStatus.IDLE) {
                throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
            }
            if (status == TrackerStatus.READY) {
                tracker.startFirstReading();
            }
            me.updateReadingStatus(ReadingStatus.READING);
        } else if (status == TrackerStatus.EXCHANGED || status == TrackerStatus.READING_2) {
            if (me.getReadingStatus() != ReadingStatus.REVIEW_DONE) {
                throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
            }
            if (status == TrackerStatus.EXCHANGED) {
                tracker.startSecondReading();
            }
            me.updateReadingStatus(ReadingStatus.READING_2);
        } else {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        publisher.publish(new TrackerNotificationEvent(READING_STARTED, user.getId(), groupId, null));
    }

    @Transactional
    public void registerReadingDone(Long groupId, User user) {
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember me = getMyMatchedMember(groupId, user.getId());

        if (tracker.getTrackerStatus() == TrackerStatus.READING
                && me.getReadingStatus() == ReadingStatus.READING) {
            me.updateReadingStatus(ReadingStatus.READ_DONE);
        } else if (tracker.getTrackerStatus() == TrackerStatus.READING_2
                && me.getReadingStatus() == ReadingStatus.READING_2) {
            me.updateReadingStatus(ReadingStatus.READ_DONE_2);
        } else {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        publisher.publish(new TrackerNotificationEvent(READING_FINISHED, user.getId(), groupId, null));
    }

    @Transactional
    public void registerExtensionDays(Long groupId, int days, User user) {
        validateGroupMember(groupId, user.getId());

        Tracker tracker = trackerRepository.findByGroupIdForUpdate(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        tracker.extensionDays(days);

        publisher.publish(new TrackerNotificationEvent(EXTEND_REQUESTED, user.getId(), groupId, tracker.getEndDate()));
    }

    // --- 택배 배송/수령 (PARCEL) ---

    @Transactional
    public void registerShipping(Long groupId, TrackerShippingRequestDTO request, User user) {
        Tracker tracker = trackerRepository.findByGroupIdForUpdate(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember me = getMyMatchedMember(groupId, user.getId());
        MatchedMember partner = getPartnerMember(groupId, me.getId());
        TrackerStatus status = tracker.getTrackerStatus();

        if (status == TrackerStatus.READ_DONE || status == TrackerStatus.EXCHANGING) {
            if (me.getReadingStatus() != ReadingStatus.REVIEW_DONE) {
                throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
            }
            if (deliveryRepository.existsByTrackerAndSenderIdAndDeliveryStatus(tracker, me.getId(), DeliveryStatus.SHIPPING)) {
                throw new TrackerException(TrackerErrorCode.ALREADY_SHIPPED);
            }
            if (status == TrackerStatus.READ_DONE) {
                tracker.startExchanging(); // READ_DONE → EXCHANGING
            }
        } else if (status == TrackerStatus.READ_DONE_2 || status == TrackerStatus.RETURNING) {
            if (me.getReadingStatus() != ReadingStatus.REVIEW_DONE_2) {
                throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
            }
            if (deliveryRepository.existsByTrackerAndSenderIdAndDeliveryStatus(tracker, me.getId(), DeliveryStatus.SHIPPING)) {
                throw new TrackerException(TrackerErrorCode.ALREADY_SHIPPED);
            }
            if (status == TrackerStatus.READ_DONE_2) {
                tracker.startReturning(); // READ_DONE_2 → RETURNING
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
        TrackerStatus status = tracker.getTrackerStatus();

        if (status != TrackerStatus.EXCHANGING && status != TrackerStatus.RETURNING) {
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
            if (status == TrackerStatus.EXCHANGING) {
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

        TrackerStatus meetingStatus = resolveMeetingStatus(tracker.getTrackerStatus());
        if (meetingStatus == null) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        return meetingRepository.findByTrackerIdAndStatusNative(tracker.getId(), meetingStatus.name())
                .map(meeting -> {
                    Location loc = meeting.getLocation();
                    return new TrackerMeetingResponseDTO(
                            meeting.getMeetingTime(),
                            loc != null ? loc.getPlaceName() : null,
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

        TrackerStatus currentStatus = tracker.getTrackerStatus();
        TrackerStatus meetingPhaseStatus = resolveMeetingStatus(currentStatus);
        if (meetingPhaseStatus == null) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        Meeting meeting = meetingRepository.findByTrackerIdAndStatusNative(tracker.getId(), meetingPhaseStatus.name())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.MEETING_NOT_FOUND));

        Location newLocation = Location.builder()
                .id(UUID.randomUUID().toString())
                .placeName(request.placeName())
                .address(request.address())
                .addressDetail(request.addressDetail())
                .zipCode(request.zipCode())
                .build();
        locationRepository.save(newLocation);

        if (meeting.getMeetingTime() != null) {
            meeting.resetConfirmation();
        }
        meeting.setMeetingDetails(newLocation, request.meetingTime());

        // 첫 번째로 미팅을 등록하면 교환/반납 단계로 진입
        if (currentStatus == TrackerStatus.READ_DONE) {
            tracker.startExchanging(); // READ_DONE → EXCHANGING
        } else if (currentStatus == TrackerStatus.READ_DONE_2) {
            tracker.startReturning(); // READ_DONE_2 → RETURNING
        }
    }

    @Transactional
    public void completeMeeting(Long groupId, User user) {
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        if (tracker.getGroup().getTradeType() != TradeType.DIRECT) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRADE_TYPE);
        }

        TrackerStatus currentStatus = tracker.getTrackerStatus();
        if (currentStatus != TrackerStatus.EXCHANGING && currentStatus != TrackerStatus.RETURNING) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        RoleStatus userRole = matchedMemberRepository.findRoleByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));

        Meeting meeting = meetingRepository.findByTrackerWithLock(tracker.getId(), currentStatus)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.MEETING_NOT_FOUND));

        meeting.confirm(userRole);

        if (meeting.isFullyConfirmed()) {
            if (currentStatus == TrackerStatus.EXCHANGING) {
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
    }

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
}
