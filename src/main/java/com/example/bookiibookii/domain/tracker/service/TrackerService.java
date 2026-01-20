package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.repository.MatchedGroupRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.tracker.converter.TrackerConverter;
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
    private final MatchedGroupRepository matchedGroupRepository;
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


}