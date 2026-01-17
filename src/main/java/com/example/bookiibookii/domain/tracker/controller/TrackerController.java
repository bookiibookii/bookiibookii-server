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

@RestController
@RequestMapping("/api/v1/trackers")
@RequiredArgsConstructor
public class TrackerController implements TrackerApi {
    private final TrackerService trackerService;

    @GetMapping("/{trackerId}")
    public ResponseEntity<TrackerDetailResponse> getTrackerDetail(@PathVariable Long trackerId) {
        return ResponseEntity.ok(trackerService.getTrackerDetail(trackerId));
    }
}
