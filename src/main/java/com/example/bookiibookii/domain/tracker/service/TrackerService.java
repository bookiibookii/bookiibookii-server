package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.tracker.converter.TrackerConverter;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequest;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerHistoryResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponse;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.entity.TrackerHistory;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.tracker.repository.TrackerHistoryRepository;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import com.example.bookiibookii.global.entity.BaseEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackerService {

    private final TrackerRepository trackerRepository;
    private final TrackerHistoryRepository trackerHistoryRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final TrackerConverter trackerConverter;



    //트래커 상세 조회
    public TrackerDetailResponse getTrackerDetailByGroupId(Long groupId) {
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        return trackerConverter.toDetailResponse(tracker);
    }


    // 트래커 히스토리 조회
    @Transactional(readOnly = true)
    public List<TrackerHistoryResponse> getTrackerHistoriesByGroupId(Long groupId) {
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
                        .map(mm -> mm.getUserId().getId())
                        .orElse(null);
            }

            // 2. receiverUserId 처리
            Long receiverUserId = null;
            if (history.getReceiverMatchedMemberId() != null) {
                receiverUserId = matchedMemberRepository.findById(history.getReceiverMatchedMemberId())
                        .map(mm -> mm.getUserId().getId())
                        .orElse(null);
            }

            // 3. 컨버터 호출
            return trackerConverter.toHistoryResponse(history, senderUserId, receiverUserId);
        }).collect(Collectors.toList());
    }


    // 트래커 리스트 조회
    @Transactional(readOnly = true)
    public List<TrackerListResponse> getTrackerList(Long userId) {
        // 한 번의 쿼리로 그룹, 트래커, 히스토리 정보를 모두 가져옴
        List<Tracker> trackers = trackerRepository.findAllByUserIdWithDetails(userId);

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
                .filter(mm -> !mm.getUserId().getId().equals(userId)) // 내가 아닌 멤버 필터링
                .map(mm -> mm.getUserId().getName()) // 유저의 이름(닉네임) 추출
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
    public void registerShipping(Long groupId, TrackerShippingRequest request) {
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        if (tracker.getTrackerStatus() != TrackerStatus.HOST_DONE &&
                tracker.getTrackerStatus() != TrackerStatus.GUEST_DONE &&
        tracker.getTrackerStatus() != TrackerStatus.READ_DONE) {
            throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
        }

        MatchedMember currentMember = tracker.getCurrentMember(); // 현재 책을 가지고 있는 사람

        int totalCapacity = tracker.getGroup().getMaxCapacity();
        // 다음 순서 계산 (예: 4명일 때 1->2->3->4->1)
        int nextOrder = (currentMember.getReadingOrder() % totalCapacity) + 1;

        // 다음 주자(receiver) 조회
        MatchedMember nextMember = matchedMemberRepository.findByGroupAndOrder(groupId, nextOrder)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NEXT_MEMBER_NOT_FOUND));

        // 엔티티에 판단 위임 (위에 작성한 메서드 호출)
        tracker.updateShippingStatus(currentMember, nextMember);

        // 트래커 히스토리에 write.
        TrackerHistory shippingHistory = tracker.createHistorySnapshot(
                currentMember.getMatchedMember(),      // 보내는 사람
                nextMember.getMatchedMember(),       // 받는 사람
                request.deliveryCompany(),
                request.trackingNumber(),
                request.authenticationImageUrl()
        );
        trackerHistoryRepository.save(shippingHistory);

    }


    //수령 완료
    @Transactional
    public void registerReceive(Long groupId) {
        // 1. 트래커 조회
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        // 2. [상태 변경] 엔티티 상태 업데이트 (SHIPPING -> RECEIVED/RETURNED)
        tracker.updateReceiveStatus();

        // 3. [새로운 단계 기록] '수령 완료' 상태가 시작되었음을 히스토리에 기록
        // 수령 완료는 배송이 아니므로 senderId는 null, receiverId는 현재 주자로 기록합니다.
        TrackerHistory receiveHistory = tracker.createHistorySnapshot(
                null,
                tracker.getCurrentMember().getMatchedMember(),
                null, null, null
        );
        trackerHistoryRepository.save(receiveHistory);
    }


    // 독서 시작
    @Transactional
    public void registerReading(Long groupId) {
        // 1. 트래커 조회
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        // 2. [상태 변경] 엔티티 상태 업데이트 (RECEIVED -> GUEST_READING 등)
        // 성진님이 짜놓으신 엔티티 내 startReading() 호출
        tracker.startReading();

        // 3. [새로운 단계 기록] '독서 중' 상태가 시작되었음을 히스토리에 기록
        // 독서 중에는 보내는 사람이 없으므로 senderId는 null, receiverId는 현재 읽는 사람(나)
        TrackerHistory readingHistory = tracker.createHistorySnapshot(
                null,
                tracker.getCurrentMember().getMatchedMember(),
                null, null, null
        );
        trackerHistoryRepository.save(readingHistory);
    }

    // 독서 완료
    @Transactional
    public void registerReadingDone(Long groupId) {
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        tracker.completeReading();

        TrackerHistory doneHistory = tracker.createHistorySnapshot(
                null,
                tracker.getCurrentMember().getMatchedMember(),
                null, null, null
        );
        trackerHistoryRepository.save(doneHistory);
    }

    // 기간 연장
    @Transactional
    public void registerExtensionDays(Long groupId, int days) {
        // 1. 트래커 조회
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        // 2. [상태/데이터 변경] 엔티티의 연장 로직 호출
        tracker.extensionDays(days);

        // 3. [새로운 단계 기록] 연장된 정보가 반영된 새로운 히스토리 생성
        TrackerHistory extensionHistory = tracker.createHistorySnapshot(
                null,
                tracker.getCurrentMember().getMatchedMember(),
                null, null, null
        );
        trackerHistoryRepository.save(extensionHistory);
    }


}