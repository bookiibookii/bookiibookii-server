package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupType;
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
import com.example.bookiibookii.domain.tracker.dto.req.TrackerReceiveRequest;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequest;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerHistoryResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerImageGetResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerMeetingResponse;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.tracker.entity.TrackerHistory;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.tracker.entity.TrackerImage;
import com.example.bookiibookii.domain.tracker.enums.TrackerImageType;
import com.example.bookiibookii.domain.tracker.exception.TrackerImageException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerImageErrorCode;
import com.example.bookiibookii.domain.tracker.repository.TrackerHistoryRepository;
import com.example.bookiibookii.domain.tracker.repository.TrackerImageRepository;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import com.example.bookiibookii.domain.user.entity.Address;
import com.example.bookiibookii.domain.user.repository.AddressRepository;
import com.example.bookiibookii.domain.userbook.entity.Card;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import com.example.bookiibookii.domain.userbook.repository.CardRepository;
import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import com.example.bookiibookii.global.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.bookiibookii.domain.tracker.enums.TrackerAction.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackerService {

    private final TrackerRepository trackerRepository;
    private final TrackerHistoryRepository trackerHistoryRepository;
    private final TrackerImageRepository trackerImageRepository;
    private final TrackerImageValidationService trackerImageValidationService;
    private final TrackerImageS3Service trackerImageS3Service;
    private final MatchedMemberRepository matchedMemberRepository;
    private final CardRepository cardRepository;
    private final UserBookRepository userBookRepository;
    private final AddressRepository addressRepository;
    private final GroupsRepository groupsRepository;
    private final MeetingRepository meetingRepository;
    private final TrackerConverter trackerConverter;
    private final DomainEventPublisher publisher;


    private void validateGroupMember(Long groupId, Long userId) {
        if (!matchedMemberRepository.existsByGroup_GroupIdAndUser_Id(groupId, userId)) {
            throw new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER); // 403 Forbidden
        }
    }

    private static final int TRACKER_IMAGE_PRESIGNED_URL_EXPIRATION_MINUTES = 10;
    private static final int TRACKER_IMAGE_GET_URL_EXPIRATION_MINUTES = 60;

    /**
     * 트래커 인증 이미지(배송/수령) 업로드용 Presigned PUT URL 발급.
     * 그룹 멤버만 발급 가능.
     */
    public PresignedUrlResponseDTO getPresignedPutUrlForTrackerImage(Long groupId, User user) {
        validateGroupMember(groupId, user.getId());
        return trackerImageS3Service.generatePresignedPutUrl(TRACKER_IMAGE_PRESIGNED_URL_EXPIRATION_MINUTES);
    }

    /**
     * 배송 인증 사진 보기. 수령한 사람(나)이 배송한 사람이 올린 SENDER_PROOF 이미지를 조회.
     * 같은 그룹 멤버만 조회 가능.
     */
    public TrackerImageGetResponse getShippingProofImageUrl(Long groupId, User user) {
        validateGroupMember(groupId, user.getId());
        MatchedMember myMatchedMember = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, user.getId())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));

        TrackerHistory shippingHistory = trackerHistoryRepository
                .findTop1ByTracker_Group_GroupIdAndReceiverMatchedMemberIdOrderByCreatedAtDesc(groupId, myMatchedMember.getId())
                .orElseThrow(() -> new TrackerImageException(TrackerImageErrorCode.TRACKING_IMAGE_NOT_FOUND));

        TrackerImage senderProof = trackerImageRepository.findByTrackerHistory_IdAndType(shippingHistory.getId(), TrackerImageType.SENDER_PROOF)
                .orElseThrow(() -> new TrackerImageException(TrackerImageErrorCode.TRACKING_IMAGE_NOT_FOUND));

        String presignedGetUrl = trackerImageS3Service.generatePresignedGetUrl(senderProof.getS3Key(), TRACKER_IMAGE_GET_URL_EXPIRATION_MINUTES);
        return TrackerImageGetResponse.builder().presignedGetUrl(presignedGetUrl).build();
    }

    /**
     * 수령 인증 사진 보기. 배송한 사람(나)이 수령한 사람이 올린 RECEIVER_PROOF 이미지를 조회.
     * 같은 그룹 멤버만 조회 가능.
     */
    public TrackerImageGetResponse getReceivedProofImageUrl(Long groupId, User user) {
        validateGroupMember(groupId, user.getId());
        MatchedMember myMatchedMember = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, user.getId())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));

        TrackerHistory myShippingHistory = trackerHistoryRepository
                .findLatestHistoryByGroupAndSender(groupId, myMatchedMember.getId())
                .orElseThrow(() -> new TrackerImageException(TrackerImageErrorCode.RECEIVED_IMAGE_NOT_FOUND));

        Long receiverMatchedMemberId = myShippingHistory.getReceiverMatchedMemberId();

        List<TrackerHistory> histories = trackerHistoryRepository.findAllByGroupId(groupId);
        List<Long> historyIds = histories.stream()
                .filter(h -> receiverMatchedMemberId.equals(h.getReceiverMatchedMemberId()))
                .map(TrackerHistory::getId)
                .toList();

        TrackerImage receiverProof = trackerImageRepository
                .findFirstByTrackerHistory_IdInAndTypeOrderByCreatedAtDesc(historyIds, TrackerImageType.RECEIVER_PROOF)
                .orElseThrow(() -> new TrackerImageException(TrackerImageErrorCode.RECEIVED_IMAGE_NOT_FOUND));

        String presignedGetUrl = trackerImageS3Service.generatePresignedGetUrl(receiverProof.getS3Key(), TRACKER_IMAGE_GET_URL_EXPIRATION_MINUTES);
        return TrackerImageGetResponse.builder().presignedGetUrl(presignedGetUrl).build();
    }


    // 트래커 생성
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createTracker(GroupMatchedEvent event) {

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

        TrackerStatus trackerStatus;
        if(group.getGroupType() == GroupType.TOGETHER){
            trackerStatus = TrackerStatus.TOGETHER;
        }else if(group.getGroupType() == GroupType.RELAY){
            trackerStatus = TrackerStatus.READY;
        }else {
            throw new GroupException(GroupErrorCode.INVALID_GROUP_TYPE);
        }

        // 4. 트래커 초기 빌드
        Tracker tracker = Tracker.builder()
                .group(group)
                .trackerStatus(trackerStatus)
                .bookOwner(firstOwner)
                .startDate(group.getStartDate().atStartOfDay())
                .endDate(group.getStartDate().atStartOfDay().plusDays(group.getReadingPeriod()))
                .extensionCount(0)
                .extensionDays(0)
                .isVerified(false)
                .build();

        trackerRepository.save(tracker);

        // 5. 첫 히스토리 기록
        TrackerHistory initialHistory = tracker.createHistorySnapshot(
                null,
                firstOwner.getId(),
                null, null
        );
        trackerHistoryRepository.save(initialHistory);

        // userbook에 트래커 사후 할당
        List<UserBook> userBooks = userBookRepository.findAllByGroup_GroupId(event.groupId());
        if (!userBooks.isEmpty()) {
            userBooks.forEach(ub -> ub.assignTracker(tracker));
        }

    }

    //트래커 상세 조회
    @Transactional(readOnly = true)
    public TrackerDetailResponse getTrackerDetailByGroupId(Long groupId, User user) {
        // 1. 권한 검증 및 트래커 조회
        validateGroupMember(groupId, user.getId());
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

           // 2. 1:1 파트너(상대방) 정보 조회
        MatchedMember partnerMember = findPartnerForRelay(groupId, user.getId());
        User partnerUser = partnerMember.getUser();

        // 3. TradeType에 따라 필요한 추가 데이터 수집
        Meeting latestMeeting = null;
        Address partnerAddress = null;
        TrackerHistory latestHistory = null;

        if (tracker.getGroup().getTradeType() == TradeType.DIRECT) {
            // [직접 교환] 최신 약속 정보 조회
            latestMeeting = meetingRepository.findLatestByGroupIdNative(groupId).orElse(null);
        } else if(tracker.getGroup().getTradeType() == TradeType.DELIVERY ) {
            // [배송] 상대방 주소 및 최신 히스토리(송장번호 등) 조회
            partnerAddress = addressRepository.findByUserId(partnerUser.getId()).orElse(null);

            TrackerStatus currentStatus = tracker.getTrackerStatus();
            if (currentStatus == TrackerStatus.SHIPPING_TO_GUEST || currentStatus == TrackerStatus.SHIPPING_TO_HOST) {

                List<TrackerStatus> shippingStatuses = List.of(
                        TrackerStatus.SHIPPING_TO_GUEST,
                        TrackerStatus.SHIPPING_TO_HOST
                );

                latestHistory = trackerHistoryRepository.findLatestShippingHistory(tracker, shippingStatuses)
                        .orElse(null);
            }
        }

        // 4. 수집된 모든 정보를 컨버터에 전달
        return trackerConverter.toDetailResponse(tracker, latestMeeting, partnerAddress, partnerUser, latestHistory);
    }

    /**
     * 1:1 교환 상황에서 현재 로그인한 유저를 제외한 파트너(MatchedMember)를 조회합니다.
     */
    private MatchedMember findPartnerForRelay(Long groupId, Long myUserId) {
        List<MatchedMember> members = matchedMemberRepository.findAllByGroup_GroupId(groupId);

        // 1:1 상황인지 데이터 수준에서 한 번 더 검증
        if (members.size() > 2) {
            throw new TrackerException(TrackerErrorCode.INVALID_PARTNER_COUNT);
        }

        return members.stream()
                .filter(mm -> !mm.getUser().getId().equals(myUserId))
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.PARTNER_NOT_FOUND));
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

    // 내 독서율 가져오기
    private int calculateUserReadingRate(Long userId, Groups group) {
        // MatchedMember에서 해당 유저의 이미 저장된 독서율 필드를 가져옵니다.
        return group.getMatchedMember().stream()
                .filter(mm -> mm.getUser().getId().equals(userId))
                .findFirst()
                .map(mm -> mm.getCurrentReadingRate() != null ? mm.getCurrentReadingRate() : 0)
                .orElse(0);
    }

    // 그룹 평균 독서율 가져오기
    private int calculateGroupAverageRate(Groups group) {
        List<MatchedMember> members = group.getMatchedMember();
        if (members == null || members.isEmpty()) return 0;

        // 루프 안에서 쿼리 실행 없이, 메모리에 로드된 멤버들의 필드값만 합산
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
                    return (nickname != null) ? nickname : "닉네임 미설정"; // null이면 기본값 반환
                })
                .findFirst()
                .orElse("상대방 없음");
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
                .map(TrackerHistory::getStartDate) // 각 상태의 start_date
                .filter(Objects::nonNull)
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

        // S3 인증 이미지 검증
        String s3Key = request.s3Key();
        validateTrackerImageS3Key(s3Key);

        // 트래커 히스토리에 write. (이미지는 TrackerImage로 저장)
        TrackerHistory shippingHistory = tracker.createHistorySnapshot(
                bookOwner.getId(),
                nextOwner.getId(),
                request.deliveryCompany(),
                request.trackingNumber()
        );
        trackerHistoryRepository.save(shippingHistory);

        TrackerImage senderProof = TrackerImage.builder()
                .trackerHistory(shippingHistory)
                .s3Key(s3Key)
                .type(TrackerImageType.SENDER_PROOF)
                .build();
        trackerImageRepository.save(senderProof);

        // 알림 publish
        publisher.publish(new TrackerNotificationEvent(SHIPPING_REGISTERED, user.getId(), groupId, null) );
    }

    private void validateTrackerImageS3Key(String s3Key) {
        if (!trackerImageValidationService.isValidS3Key(s3Key)) {
            throw new TrackerImageException(TrackerImageErrorCode.INVALID_S3_KEY_FORMAT);
        }
        if (!trackerImageS3Service.doesImageExist(s3Key)) {
            throw new TrackerImageException(TrackerImageErrorCode.IMAGE_NOT_FOUND_IN_S3);
        }
        if (trackerImageRepository.existsByS3Key(s3Key)) {
            throw new TrackerImageException(TrackerImageErrorCode.DUPLICATE_S3_KEY);
        }
    }


    //수령 완료
    @Transactional
    public void registerReceive(Long groupId, TrackerReceiveRequest request, User user) {
        // 1. 트래커 조회
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember bookOwner = tracker.getBookOwner();
        // 권한 검증
        if (!bookOwner.getUser().getId().equals(user.getId())) {
            throw new TrackerException(TrackerErrorCode.NOT_TRACKER_OWNER);
        }

        // S3 수령 인증 이미지 검증
        String s3Key = request.s3Key();
        validateTrackerImageS3Key(s3Key);

        // 2. [상태 변경] 엔티티 상태 업데이트 (SHIPPING -> RECEIVED/RETURNED)
        tracker.updateReceiveStatus();

        // 3. [새로운 단계 기록] '수령 완료' 상태가 시작되었음을 히스토리에 기록
        TrackerHistory receiveHistory = tracker.createHistorySnapshot(
                null,
                bookOwner.getId(),
                null, null
        );
        trackerHistoryRepository.save(receiveHistory);

        TrackerImage receiverProof = TrackerImage.builder()
                .trackerHistory(receiveHistory)
                .s3Key(s3Key)
                .type(TrackerImageType.RECEIVER_PROOF)
                .build();
        trackerImageRepository.save(receiverProof);

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
        tracker.startReading();

        // 3. [새로운 단계 기록] '독서 중' 상태가 시작되었음을 히스토리에 기록
        // 독서 중에는 보내는 사람이 없으므로 senderId는 null, receiverId는 현재 읽는 사람(나)
        TrackerHistory readingHistory = tracker.createHistorySnapshot(
                null,
                bookOwner.getId(),
                null, null
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
                bookOwner.getId(),
                null, null
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

        RoleStatus roleStatus= bookOwner.getRole();

        // 2. [상태/데이터 변경] 엔티티의 연장 로직 호출
        tracker.extensionDays(days, roleStatus);

        // 3. [새로운 단계 기록] 연장된 정보가 반영된 새로운 히스토리 생성
        TrackerHistory extensionHistory = tracker.createHistorySnapshot(
                null,
                bookOwner.getId(),
                null, null
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
        return meetingRepository.findLatestByGroupIdNative(groupId)
                .map(meeting -> new TrackerMeetingResponse(
                        meeting.getMeetingTime(),
                        meeting.getMeetingPlace()
                ))
                .orElseGet(() -> {
                    // 약속 테이블 자체가 비어있을 때만 호스트의 선호 장소 반환
                    String defaultPlace = tracker.getGroup().getPreferRegion();
                    return new TrackerMeetingResponse(null, defaultPlace);
                });
    }


    //약속 정보 업데이트
    @Transactional
    public void updateMeeting(Long groupId, TrackerMeetingRequest request, User user) {
        meetingRepository.flush();
        // 1. 트래커 조회 및 권한 검증
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        // 현재 책 소유자 확인
        MatchedMember bookOwner = tracker.getBookOwner();
        if (!bookOwner.getUser().getId().equals(user.getId())) {
            throw new TrackerException(TrackerErrorCode.NOT_TRACKER_OWNER);
        }

        if (tracker.getGroup().getTradeType() != TradeType.DIRECT) {
             throw new TrackerException(TrackerErrorCode.INVALID_TRADE_TYPE);
        }

        TrackerStatus currentStatus = tracker.getTrackerStatus();
        if (currentStatus != TrackerStatus.HOST_DONE &&
                currentStatus != TrackerStatus.GUEST_DONE &&
                currentStatus != TrackerStatus.READ_DONE) {
                throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        // 현재 소유자가 호스트면 '전달(GUEST)' 단계, 게스트면 '반납(HOST)' 단계로 정의
        TrackerStatus meetingStep = (tracker.getBookOwner().getRole() == RoleStatus.HOST)
                ? TrackerStatus.SHIPPING_TO_GUEST : TrackerStatus.SHIPPING_TO_HOST;

        Meeting meeting = meetingRepository.findByGroupIdAndStatusNative(groupId, meetingStep.name())
                .orElse(null);

        if (meeting == null) {
            meeting = Meeting.builder()
                    .group(tracker.getGroup())
                    .trackerStatus(meetingStep)
                    .meetingPlace(request.meetingPlace()) // Autofilled 된 값 그대로 저장
                    .meetingTime(request.meetingTime())
                    .build();
        } else {
            meeting.resetConfirmation();

        }

        tracker.updateStatus(meetingStep);
        meeting.setMeetingDetails(request.meetingPlace(), request.meetingTime());

        meetingRepository.saveAndFlush(meeting);

        int totalCapacity = tracker.getGroup().getMaxCapacity();
        // 다음 순서 계산 (예: 4명일 때 1->2->3->4->1)
        int nextOrder = (bookOwner.getReadingOrder() % totalCapacity) + 1;

        // 다음 주자(receiver) 조회
        MatchedMember nextOwner = matchedMemberRepository.findByGroupAndOrder(groupId, nextOrder)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NEXT_MEMBER_NOT_FOUND));

        TrackerHistory meetingHistory = tracker.createHistorySnapshot(
                bookOwner.getId(),
                nextOwner.getId(),
                null, null
        );
        trackerHistoryRepository.save(meetingHistory);
    }

    // 약속 완료
    @Transactional
    public void completeMeeting(Long groupId, User user) {

        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        if (tracker.getGroup().getTradeType() != TradeType.DIRECT) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRADE_TYPE);
        }
        if (tracker.getTrackerStatus() != TrackerStatus.SHIPPING_TO_GUEST &&
                tracker.getTrackerStatus() != TrackerStatus.SHIPPING_TO_HOST) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        RoleStatus userRole = matchedMemberRepository.findRoleByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));

        Meeting meeting = meetingRepository.findByGroupWithLock(groupId, tracker.getTrackerStatus())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.MEETING_NOT_FOUND));


        meeting.confirm(userRole);

        if (meeting.isFullyConfirmed()) {
            processStatusTransition(tracker);
        } else {
            // 아직 한 명만 확인한 상태일 때
            log.info("상대방 확인 대기 중.");
        }
    }

    private void processStatusTransition(Tracker tracker) {
        MatchedMember currentOwner = tracker.getBookOwner();
        Long groupId = tracker.getGroup().getGroupId();
        int totalCapacity = tracker.getGroup().getMaxCapacity();

        int nextOrder = (currentOwner.getReadingOrder() % totalCapacity) + 1;
        MatchedMember nextOwner = matchedMemberRepository.findByGroupAndOrder(groupId, nextOrder)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NEXT_MEMBER_NOT_FOUND));

        TrackerStatus nextStatus = (currentOwner.getRole() == RoleStatus.HOST)
                ? TrackerStatus.RECEIVED : TrackerStatus.RETURNED;

        tracker.completeTrade(nextOwner, nextStatus);

        TrackerHistory transitionHistory = tracker.createHistorySnapshot(
                currentOwner.getId(),
                nextOwner.getId(),
                null, null
        );
        trackerHistoryRepository.save(transitionHistory);

        // 알림 발송
        publisher.publish(new TrackerNotificationEvent(RECEIVED_CONFIRMED, nextOwner.getUser().getId(), groupId, null));
    }


    @Transactional
    public void verifyPartnerReception(Long groupId, User user) {
        // 1. groupId로 해당 그룹의 트래커 조회
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        // 2. 현재 트래커의 주인(책을 받은 사람) 정보 가져오기
        Long currentOwnerUserId = tracker.getBookOwner().getUser().getId();

        // 🟢 3. 권한 검증: 현재 주인(받은 사람)은 본인의 수령 사진을 확인할 수 없음
        // 즉, 반대편에 있는 사람(보낸 사람)이 확인 버튼을 눌러야 함
        if (currentOwnerUserId.equals(user.getId())) {
            throw new TrackerException(TrackerErrorCode.OWNER_CANNOT_VERIFY);
        }

        // 4. 요청자가 해당 그룹의 멤버인지 최종 확인 (보안 강화)
        boolean isMember = tracker.getGroup().getMatchedMember().stream()
                .anyMatch(mm -> mm.getUser().getId().equals(user.getId()));

        if (!isMember) {
            throw new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER);
        }

        // 5. 엔티티 메서드 호출 (isVerified = true)
        tracker.verifyReception();
    }

}