package com.example.bookiibookii.domain.tracker.controller;


import com.example.bookiibookii.domain.tracker.controller.docs.TrackerApi;
import com.example.bookiibookii.domain.tracker.dto.response.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.service.TrackerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//@RestController
//@RequestMapping("/api/v1/trackers")
//@RequiredArgsConstructor
//public class TrackerController implements TrackerApi {
//    private final TrackerService trackerService;
//
//    @GetMapping("/{trackerId}")
//    public ResponseEntity<TrackerDetailResponse> getTrackerDetail(@PathVariable Long trackerId) {
//        return ResponseEntity.ok(trackerService.getTrackerDetail(trackerId));
//    }
//}

@RestController
@RequestMapping("/api/groups") // 주소는 groups로 시작!
@RequiredArgsConstructor
public class TrackerController implements TrackerApi {

    private final TrackerService trackerService;

    // 상세 현황 조회: GET /api/v1/groups/{groupId}/tracker
    @GetMapping("/{groupId}/tracker")
    public ResponseEntity<TrackerDetailResponse> getTrackerDetail(@PathVariable Long groupId) {
        return ResponseEntity.ok(trackerService.getTrackerDetailByGroupId(groupId));
    }

    // 히스토리 조회: GET /api/v1/groups/{groupId}/tracker/histories
//    @GetMapping("/{groupId}/tracker/histories")
//    public ResponseEntity<List<TrackerHistoryResponse>> getTrackerHistories(@PathVariable Long groupId) {
//        return ResponseEntity.ok(trackerService.getTrackerHistoriesByGroupId(groupId));
//    }
}