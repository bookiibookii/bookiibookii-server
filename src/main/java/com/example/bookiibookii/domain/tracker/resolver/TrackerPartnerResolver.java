package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import org.springframework.stereotype.Component;

@Component
public class TrackerPartnerResolver {

    // 상대방 찾기
    public MatchedMember resolve(Groups group, MatchedMember me) {
        return group.getMatchedMember().stream()
                .filter(matchedMember -> !matchedMember.getId().equals(me.getId()))
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.PARTNER_NOT_FOUND));
    }
}