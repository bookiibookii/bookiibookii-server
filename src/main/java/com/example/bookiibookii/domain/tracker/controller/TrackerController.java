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
import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequest;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerHistoryResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponse;
import com.example.bookiibookii.domain.tracker.service.TrackerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class TrackerController implements TrackerApi {

    private final TrackerService trackerService;

    /**
     * 내 트래커 리스트 조회
     * 테스트용 임시 유저 ID: 1L
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

    @Override
    @PostMapping("/{groupId}/tracker/shipping")
    public ResponseEntity<TrackerDetailResponse> registerShipping(
            @PathVariable Long groupId,
            @RequestBody @Valid TrackerShippingRequest request
    ) {
        trackerService.registerShipping(groupId, request);
        // 배송 등록 후 바뀐 상태와 다음 주자 정보를 포함해 응답
        return ResponseEntity.ok(trackerService.getTrackerDetailByGroupId(groupId));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/receive")
    public ResponseEntity<TrackerDetailResponse> registerReceive(@PathVariable Long groupId) {
        trackerService.registerReceive(groupId);
        // 수령 후 바뀐 상태(RECEIVED) 응답
        return ResponseEntity.ok(trackerService.getTrackerDetailByGroupId(groupId));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/reading")
    public ResponseEntity<TrackerDetailResponse> registerReading(@PathVariable Long groupId) {
        trackerService.registerReading(groupId);
        // 독서 시작 시 계산된 'endDate(반납예정일)'를 보여주기 위해 상세 정보 응답
        return ResponseEntity.ok(trackerService.getTrackerDetailByGroupId(groupId));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/extension")
    public ResponseEntity<TrackerDetailResponse> registerExtension(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "3") int days
    ) {
        trackerService.registerExtensionDays(groupId, days);
        // 연장 후 늘어난 'endDate'와 'extensionCount' 확인을 위해 상세 정보 응답
        return ResponseEntity.ok(trackerService.getTrackerDetailByGroupId(groupId));
    }

    @Override
    @PatchMapping("/{groupId}/tracker/done")
    public ResponseEntity<TrackerDetailResponse> registerReadingDone(@PathVariable Long groupId) {
        trackerService.registerReadingDone(groupId);
        // 독서 완료 후에는 상태값만 내려주거나 상세 정보를 내려주어도 무방함
        return ResponseEntity.ok(trackerService.getTrackerDetailByGroupId(groupId));
    }
}