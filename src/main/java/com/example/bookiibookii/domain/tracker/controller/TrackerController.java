//package com.example.bookiibookii.domain.tracker.controller;
//
//
//
//import com.example.bookiibookii.domain.tracker.controller.docs.TrackerApi;
//import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponse;
//import com.example.bookiibookii.domain.tracker.dto.res.TrackerHistoryResponse;
//import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponse;
//import com.example.bookiibookii.domain.tracker.service.TrackerService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/groups") // 주소는 groups로 시작!
//@RequiredArgsConstructor
//public class TrackerController implements TrackerApi {
//
//    private final TrackerService trackerService;
//
//    // 상세 현황 조회: GET /api/groups/{groupId}/tracker
//    @GetMapping("/{groupId}/tracker")
//    public ResponseEntity<TrackerDetailResponse> getTrackerDetail(@PathVariable Long groupId) {
//        return ResponseEntity.ok(trackerService.getTrackerDetailByGroupId(groupId));
//    }
//
//    // 히스토리 조회: GET /api/groups/{groupId}/tracker/histories
//    @GetMapping("/{groupId}/tracker/histories")
//    public ResponseEntity<List<TrackerHistoryResponse>> getTrackerHistories(@PathVariable Long groupId) {
//        return ResponseEntity.ok(trackerService.getTrackerHistoriesByGroupId(groupId));
//    }
//
//    // 추가 - 트래커 리스트 조회: GET /api/groups/me/trackers
//    @GetMapping("/me/trackers")
//    public ResponseEntity<List<TrackerListResponse>> getTrackerList(Long userId) {
//        return ResponseEntity.ok(trackerService.getTrackerList(userId));
//    }
//
//}
//


package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.controller.docs.TrackerApi;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerHistoryResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponse;
import com.example.bookiibookii.domain.tracker.service.TrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class TrackerController implements TrackerApi {

    private final TrackerService trackerService;

    /**
     * 내 트래커 리스트 조회
     * SDK 미연동 상태이므로 테스트를 위해 ID 1번 유저로 하드코딩하여 처리합니다.
     */
    @Override
    @GetMapping("/me/trackers")
    public ResponseEntity<List<TrackerListResponse>> getTrackerList() {
        Long tempUserId = 1L;
        return ResponseEntity.ok(trackerService.getTrackerList(tempUserId));
    }

    @Override
    @GetMapping("/{groupId}/tracker")
    public ResponseEntity<TrackerDetailResponse> getTrackerDetail(@PathVariable Long groupId) {
        return ResponseEntity.ok(trackerService.getTrackerDetailByGroupId(groupId));
    }

    @Override
    @GetMapping("/{groupId}/tracker/histories")
    public ResponseEntity<List<TrackerHistoryResponse>> getTrackerHistories(@PathVariable Long groupId) {
        return ResponseEntity.ok(trackerService.getTrackerHistoriesByGroupId(groupId));
    }
}
