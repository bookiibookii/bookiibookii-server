package com.example.bookiibookii.domain.aladin.controller;

import com.example.bookiibookii.domain.aladin.scheduler.BestsellerScheduler;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// todo : 테스트용이므로 나중에 삭제 필요
@RestController
@Profile({"local", "dev"})
@RequestMapping("/api/dev/admin/bestsellers")
@RequiredArgsConstructor
public class DevBestsellerController {

    private final BestsellerScheduler bestsellerScheduler;

    @PostMapping("/refresh")
    public ApiResponse<RefreshResult> refresh() {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                new RefreshResult(bestsellerScheduler.refreshBestsellers())
        );
    }

    public record RefreshResult(int savedCount) {
    }
}
