package com.example.bookiibookii.global.scheduler;

import com.example.bookiibookii.domain.group.scheduler.GroupScheduler;
import com.example.bookiibookii.domain.tracker.scheduler.DeliveryReceiveReminderScheduler;
import com.example.bookiibookii.domain.tracker.scheduler.DirectExchangeReminderScheduler;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "dev"})
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/dev/admin/scheduler")
@RequiredArgsConstructor
public class DevSchedulerController {

    private final WithdrawnUserCleanupScheduler withdrawnUserCleanupScheduler;
    private final GroupScheduler groupScheduler;
    private final DirectExchangeReminderScheduler directExchangeReminderScheduler;
    private final DeliveryReceiveReminderScheduler deliveryReceiveReminderScheduler;

    @PostMapping("/withdrawn-user/cleanup")
    public ApiResponse<Void> runWithdrawnUserCleanup() {
        withdrawnUserCleanupScheduler.deleteExpiredWithdrawnUsers();
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @PostMapping("/group/force-complete")
    public ApiResponse<Void> runGroupForceComplete() {
        groupScheduler.forceCompleteGroups();
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @PostMapping("/direct-exchange/reminder")
    public ApiResponse<Void> runDirectExchangeReminder() {
        directExchangeReminderScheduler.sendOverdueMeetingReminders();
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @PostMapping("/delivery/receive-reminder")
    public ApiResponse<Void> runDeliveryReceiveReminder() {
        deliveryReceiveReminderScheduler.sendReceiveReminders();
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}
