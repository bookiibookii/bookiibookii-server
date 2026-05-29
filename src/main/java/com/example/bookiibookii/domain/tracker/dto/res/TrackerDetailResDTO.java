package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.tracker.dto.BookInfo;
import com.example.bookiibookii.domain.tracker.dto.TrackerStepInfo;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record TrackerDetailResDTO(
        Long groupId,
        String groupName,
        TradeType tradeType,
        RoleStatus myRole,

        TrackerDisplayStatus displayStatus,
        String displayStatusText,
        Integer dDay,

        BookInfo myBook,
        BookInfo partnerBook,

        List<TrackerStepInfo> steps
) {}
