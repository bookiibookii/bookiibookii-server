package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.repository.MatchedGroupRepository;
import com.example.bookiibookii.domain.tracker.converter.TrackerConverter;
import com.example.bookiibookii.domain.tracker.dto.response.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.response.TrackerHistoryResponse;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.entity.TrackerHistory;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.tracker.repository.TrackerHistoryRepository;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackerService {

    private final TrackerRepository trackerRepository;
    private final TrackerHistoryRepository trackerHistoryRepository;
    private final MatchedGroupRepository matchedGroupRepository;
    private final TrackerConverter trackerConverter;


    public TrackerDetailResponse getTrackerDetailByGroupId(Long groupId) {
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        return trackerConverter.toDetailResponse(tracker);
    }


    @Transactional(readOnly = true)
    public List<TrackerHistoryResponse> getTrackerHistoriesByGroupId(Long groupId) {
        // 1. 해당 그룹의 모든 히스토리 조회
        List<TrackerHistory> histories = trackerHistoryRepository.findAllByGroupId(groupId);

        // 리스트가 비어있다면 커스텀 예외 발생
        if (histories.isEmpty()) {
            throw new TrackerException(TrackerErrorCode.HISTORY_NOT_FOUND);
        }

        return histories.stream().map(history -> {
            // 2. senderMatchedMemberId를 이용해 MatchedMember 엔티티 조회 -> User ID 추출
            Long senderUserId = matchedGroupRepository.findById(history.getSenderMatchedMemberId())
                    .map(mm -> mm.getUserId().getId())
                    .orElse(null);

            // 3. receiverMatchedMemberId를 이용해 MatchedMember 엔티티 조회 -> User ID 추출
            Long receiverUserId = matchedGroupRepository.findById(history.getReceiverMatchedMemberId())
                    .map(mm -> mm.getUserId().getId())
                    .orElse(null);

            // 4. 컨버터에 추출한 ID들을 함께 전달
            return trackerConverter.toHistoryResponse(history, senderUserId, receiverUserId);
        }).collect(Collectors.toList());
    }
}