package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.event.GroupMatchedEvent;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.tracker.converter.TrackerConverter;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerMeetingRequest;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequest;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerHistoryResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerMeetingResponse;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.entity.TrackerHistory;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.tracker.repository.TrackerHistoryRepository;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.example.bookiibookii.domain.tracker.enums.TrackerAction.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackerService {

    private final TrackerRepository trackerRepository;
    private final TrackerHistoryRepository trackerHistoryRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final GroupsRepository groupsRepository;
    private final MeetingRepository meetingRepository;
    private final TrackerConverter trackerConverter;
    private final DomainEventPublisher publisher;


    private void validateGroupMember(Long groupId, Long userId) {
        if (!matchedMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, userId)) {
            throw new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER); // 403 Forbidden
        }
    }


    // 트래커 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTracker(GroupMatchedEvent event) {
        log.info("[TrackerService] createTracker 시작 - groupId: {}, hostId: {}", event.groupId(), event.hostId());

        // 1. 이미 해당 그룹의 트래커가 있는지 검증 (boolean 체크)
        if (trackerRepository.existsByGroup_GroupId(event.groupId())) {
            throw new TrackerException(TrackerErrorCode.TRACKER_ALREADY_EXISTS);
        }

        // 2. 그룹 엔티티 조회
        Groups group = groupsRepository.findById(event.groupId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        // 3. 첫 번째 주자(호스트, 순서 1번)의 MatchedMember 조회
        MatchedMember firstOwner = matchedMemberRepository.findByGroupAndOrder(event.groupId(), 1)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.FIRST_MEMBER_NOT_FOUND));

        // 4. 트래커 초기 빌드
        Tracker tracker = Tracker.builder()
                .group(group)
                .trackerStatus(TrackerStatus.READY)
                .bookOwner(firstOwner)
                .startDate(group.getStartDate().atStartOfDay())
                .endDate(group.getStartDate().atStartOfDay().plusDays(group.getReadingPeriod()))
                .extensionCount(0)
                .extensionDays(0)
                .build();

        trackerRepository.save(tracker);
        log.info("[TrackerService] 트래커 엔티티 저장 완료 - trackerId: {}", tracker.getId());

        // 5. 첫 히스토리 기록
        TrackerHistory initialHistory = tracker.createHistorySnapshot(
                null,
                firstOwner.getMatchedMember(),
                null, null, null
        );
        trackerHistoryRepository.save(initialHistory);

        log.info("[TrackerService] 초기 히스토리 저장 완료 - historyId: {}", initialHistory.getId());
    }

    //트래커 상세 조회
    public TrackerDetailResponse getTrackerDetailByGroupId(Long groupId, User user) {
        validateGroupMember(groupId, user.getId());
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        return trackerConverter.toDetailResponse(tracker);
    }


    // 트래커 히스토리 조회
    @Transactional(readOnly = true)
    public List<TrackerHistoryResponse> getTrackerHistoriesByGroupId(Long groupId, User user) {
        validateGroupMember(groupId, user.getId());

        // 1. 해당 그룹의 모든 히스토리 조회
        List<TrackerHistory> histories = trackerHistoryRepository.findAllByGroupId(groupId);

        // 리스트가 비어있다면 커스텀 예외 발생
        if (histories.isEmpty()) {
            throw new TrackerException(TrackerErrorCode.HISTORY_NOT_FOUND);
        }

        return histories.stream().map(history -> {
            // 1. senderUserId 처리 (null 체크 필수!)
            Long senderUserId = null;
            if (history.getSenderMatchedMemberId() != null) {
                // (주의) matchedGroupRepository가 아니라 matchedMemberRepository를 사용해야 할 것 같습니다.
                senderUserId = matchedMemberRepository.findById(history.getSenderMatchedMemberId())
                        .map(mm -> mm.getUser().getId())
                        .orElse(null);
            }

            // 2. receiverUserId 처리
            Long receiverUserId = null;
            if (history.getReceiverMatchedMemberId() != null) {
                receiverUserId = matchedMemberRepository.findById(history.getReceiverMatchedMemberId())
                        .map(mm -> mm.getUser().getId())
                        .orElse(null);
            }

            // 3. 컨버터 호출
            return trackerConverter.toHistoryResponse(history, senderUserId, receiverUserId);
        }).collect(Collectors.toList());
    }


    // 트래커 리스트 조회
    // 1. 전체 조회
    @Transactional(readOnly = true)
    public List<TrackerListResponse> getTrackerList(Long userId) {
        List<Tracker> trackers = trackerRepository.findAllByUserIdWithDetails(userId);
        return convertToResponseList(trackers, userId);
    }

    // 2. 내가 호스트인 리스트 조회
    @Transactional(readOnly = true)
    public List<TrackerListResponse> getHostTrackerList(Long userId) {
        List<Tracker> trackers = trackerRepository.findAllByUserIdAndRoleWithDetails(userId, RoleStatus.HOST);
        return convertToResponseList(trackers, userId);
    }

    // 3.  내가 게스트인 리스트 조회
    @Transactional(readOnly = true)
    public List<TrackerListResponse> getGuestTrackerList(Long userId) {
        List<Tracker> trackers = trackerRepository.findAllByUserIdAndRoleWithDetails(userId, RoleStatus.GUEST);
        return convertToResponseList(trackers, userId);
    }

    // 공통 변환 로직
    private List<TrackerListResponse> convertToResponseList(List<Tracker> trackers, Long userId) {
        return trackers.stream()
                .map(tracker -> {
                    List<String> stepDates = buildStepDates(tracker);
                    String targetNickname = findTargetNickname(tracker, userId);
                    return trackerConverter.toListResponse(tracker, targetNickname, stepDates);
                })
                .collect(Collectors.toList());
    }


    private String findTargetNickname(Tracker tracker, Long userId) {
        return tracker.getGroup().getMatchedMember().stream()
                .filter(mm -> !mm.getUser().getId().equals(userId)) // 내가 아닌 멤버 필터링
                .map(mm -> mm.getUser().getName()) // 유저의 이름(닉네임) 추출
                .findFirst()
                .orElse("상대방 없음"); // 만약 멤버가 혼자라면 기본값 반환
    }

    private List<String> buildStepDates(Tracker tracker) {
        List<String> dates = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM. dd.");

        List<TrackerHistory> histories = tracker.getHistories();
            if (histories == null || histories.isEmpty()) {
                 return dates;
              }

        addDateIfPresent(dates, histories, TrackerStatus.HOST_READING, formatter);    // 호스트 읽는중
        addDateIfPresent(dates, histories, TrackerStatus.SHIPPING_TO_GUEST, formatter);    // 배송 중
        addDateIfPresent(dates, histories, TrackerStatus.GUEST_READING, formatter); // 게스트 읽는 중
        addDateIfPresent(dates, histories, TrackerStatus.SHIPPING_TO_HOST, formatter);   // 회수 중

        return dates;
    }

    private void addDateIfPresent(List<String> dates, List<TrackerHistory> histories, TrackerStatus status, DateTimeFormatter formatter) {
        histories.stream()
                .filter(h -> h.getTrackerStatus() == status)
                .map(BaseEntity::getCreatedAt) // 히스토리가 생성된 시점
                .sorted() // 혹시 모를 중복 기록에 대비해 가장 빠른 날짜 선택
                .findFirst()
                .ifPresent(createdAt -> dates.add(createdAt.format(formatter)));
    }


    // 배송 등록
    @Transactional
    public void registerShipping(Long groupId, TrackerShippingRequest request, User user) {
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        if (tracker.getTrackerStatus() != TrackerStatus.HOST_DONE &&
                tracker.getTrackerStatus() != TrackerStatus.GUEST_DONE &&
        tracker.getTrackerStatus() != TrackerStatus.READ_DONE) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        MatchedMember bookOwner = tracker.getBookOwner(); // 현재 책을 가지고 있는 사람

        // 권한 검증
        if(!bookOwner.getUser().getId().equals(user.getId())){
            throw new TrackerException(TrackerErrorCode.NOT_TRACKER_OWNER);
        }

        int totalCapacity = tracker.getGroup().getMaxCapacity();
        // 다음 순서 계산 (예: 4명일 때 1->2->3->4->1)
        int nextOrder = (bookOwner.getReadingOrder() % totalCapacity) + 1;

        // 다음 주자(receiver) 조회
        MatchedMember nextOwner = matchedMemberRepository.findByGroupAndOrder(groupId, nextOrder)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NEXT_MEMBER_NOT_FOUND));

        // 엔티티에 판단 위임 (위에 작성한 메서드 호출)
        tracker.updateShippingStatus(bookOwner, nextOwner);

        // 트래커 히스토리에 write.
        TrackerHistory shippingHistory = tracker.createHistorySnapshot(
                bookOwner.getMatchedMember(),      // 보내는 사람
                nextOwner.getMatchedMember(),       // 받는 사람
                request.deliveryCompany(),
                request.trackingNumber(),
                request.authenticationImageUrl()
        );
        trackerHistoryRepository.save(shippingHistory);

        // 알림 publish
        publisher.publish(new TrackerNotificationEvent(SHIPPING_REGISTERED, user.getId(), groupId, null) );
    }


    //수령 완료
    @Transactional
    public void registerReceive(Long groupId, User user) {
        // 1. 트래커 조회
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember bookOwner = tracker.getBookOwner();
        // 권한 검증
        if(!bookOwner.getUser().getId().equals(user.getId())){
            throw new TrackerException(TrackerErrorCode.NOT_TRACKER_OWNER);
        }


        // 2. [상태 변경] 엔티티 상태 업데이트 (SHIPPING -> RECEIVED/RETURNED)
        tracker.updateReceiveStatus();

        // 3. [새로운 단계 기록] '수령 완료' 상태가 시작되었음을 히스토리에 기록
        // 수령 완료는 배송이 아니므로 senderId는 null, receiverId는 현재 주자로 기록합니다.
        TrackerHistory receiveHistory = tracker.createHistorySnapshot(
                null,
                bookOwner.getMatchedMember(),
                null, null, null
        );
        trackerHistoryRepository.save(receiveHistory);

        // 알림 publish
        publisher.publish(new TrackerNotificationEvent(RECEIVED_CONFIRMED, user.getId(), groupId, null) );
    }


    // 독서 시작
    @Transactional
    public void registerReading(Long groupId, User user) {
        // 1. 트래커 조회
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember bookOwner = tracker.getBookOwner();
        // 권한 검증
        if(!bookOwner.getUser().getId().equals(user.getId())){
            throw new TrackerException(TrackerErrorCode.NOT_TRACKER_OWNER);
        }


        // 2. [상태 변경] 엔티티 상태 업데이트 (RECEIVED -> GUEST_READING 등)
        // 성진님이 짜놓으신 엔티티 내 startReading() 호출
        tracker.startReading();

        // 3. [새로운 단계 기록] '독서 중' 상태가 시작되었음을 히스토리에 기록
        // 독서 중에는 보내는 사람이 없으므로 senderId는 null, receiverId는 현재 읽는 사람(나)
        TrackerHistory readingHistory = tracker.createHistorySnapshot(
                null,
                bookOwner.getMatchedMember(),
                null, null, null
        );
        trackerHistoryRepository.save(readingHistory);

        // 알림 publish
        publisher.publish(new TrackerNotificationEvent(READING_STARTED, user.getId(), groupId, null) );
    }

    // 독서 완료
    @Transactional
    public void registerReadingDone(Long groupId, User user) {
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember bookOwner = tracker.getBookOwner();
        // 권한 검증
        if(!bookOwner.getUser().getId().equals(user.getId())){
            throw new TrackerException(TrackerErrorCode.NOT_TRACKER_OWNER);
        }

        tracker.completeReading();

        TrackerHistory doneHistory = tracker.createHistorySnapshot(
                null,
                bookOwner.getMatchedMember(),
                null, null, null
        );
        trackerHistoryRepository.save(doneHistory);

        // 알림 publish
        publisher.publish(new TrackerNotificationEvent(READING_FINISHED, user.getId(), groupId, null) );
    }

    // 기간 연장
    @Transactional
    public void registerExtensionDays(Long groupId, int days, User user) {
        // 1. 트래커 조회
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));
        MatchedMember bookOwner = tracker.getBookOwner();
        // 권한 검증
        if(!bookOwner.getUser().getId().equals(user.getId())){
            throw new TrackerException(TrackerErrorCode.NOT_TRACKER_OWNER);
        }

        // 2. [상태/데이터 변경] 엔티티의 연장 로직 호출
        tracker.extensionDays(days);

        // 3. [새로운 단계 기록] 연장된 정보가 반영된 새로운 히스토리 생성
        TrackerHistory extensionHistory = tracker.createHistorySnapshot(
                null,
                bookOwner.getMatchedMember(),
                null, null, null
        );
        trackerHistoryRepository.save(extensionHistory);

        // 알림 publish
        publisher.publish(new TrackerNotificationEvent(EXTEND_REQUESTED, user.getId(), groupId, tracker.getEndDate()) );
    }

    // 약속 상세 조회 (현재 트래커 상태에 맞는 약속 조회)
    public TrackerMeetingResponse getMeetingDetailByGroupId(Long groupId, User user) {
        validateGroupMember(groupId, user.getId());

        // 1. 현재 트래커 조회 (상태 확인을 위함)
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

          if (tracker.getGroup().getTradeType() != TradeType.DIRECT) {
                 throw new TrackerException(TrackerErrorCode.INVALID_TRADE_TYPE);}

        TrackerStatus currentStatus = tracker.getTrackerStatus();

        // 2. 현재 단계에 등록된 약속이 있는지 확인
        return meetingRepository.findByGroup_GroupIdAndTrackerStatus(groupId, currentStatus)
                .map(meeting -> new TrackerMeetingResponse(
                        meeting.getMeetingTime(),
                        meeting.getMeetingPlace()
                ))
                // 3. 약속이 없다면(orElseGet), 게스트의 선호 장소를 기본값으로 제공
                .orElseGet(() -> {
                    String defaultPlace = tracker.getGroup().getHost().getMeetPlace();
                    return new TrackerMeetingResponse(null, defaultPlace);
                });
    }



    @Transactional
    public void updateMeeting(Long groupId, TrackerMeetingRequest request, User user) {

        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember bookOwner = tracker.getBookOwner();
        if (!bookOwner.getUser().getId().equals(user.getId())) {
            throw new TrackerException(TrackerErrorCode.NOT_TRACKER_OWNER);
        }

        // 2. 전달 방식 검증 (직접 교환만 가능)
        if (tracker.getGroup().getTradeType() != TradeType.DIRECT) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRADE_TYPE);
        }

        // 3. 현재 트래커 상태를 키로 사용하여 Meeting 조회 또는 생성
        TrackerStatus currentStatus = tracker.getTrackerStatus();


        Meeting meeting = meetingRepository.findByGroup_GroupIdAndTrackerStatus(groupId, currentStatus)
                .orElseGet(() -> Meeting.builder()
                        .group(tracker.getGroup())
                        .trackerStatus(currentStatus)
                        .meetingPlace(tracker.getGroup().getHost().getMeetPlace())
                        .build());

        // 4. 데이터 업데이트 및 저장
        meeting.setMeetingDetails(request.meetingPlace(), request.meetingTime());
        meetingRepository.save(meeting);
    }
}