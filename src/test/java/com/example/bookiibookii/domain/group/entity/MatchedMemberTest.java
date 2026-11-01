package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchedMemberTest {

    @Test
    void singleArgumentReadingStatusUpdateRejectsPartnerReviewingTransition() {
        MatchedMember member = MatchedMember.builder()
                .readingStatus(ReadingStatus.RETURNING)
                .build();

        assertThatThrownBy(() -> member.updateReadingStatus(ReadingStatus.PARTNER_REVIEWING))
                .isInstanceOf(TrackerException.class);
        assertThat(member.getReadingStatus()).isEqualTo(ReadingStatus.RETURNING);
        assertThat(member.getPartnerReviewingStartedAt()).isNull();
    }

    @Test
    void partnerReviewingTransitionRequiresChangedAt() {
        MatchedMember member = MatchedMember.builder()
                .readingStatus(ReadingStatus.RETURNING)
                .build();

        assertThatThrownBy(() -> member.updateReadingStatus(ReadingStatus.PARTNER_REVIEWING, null))
                .isInstanceOf(TrackerException.class);
        assertThat(member.getReadingStatus()).isEqualTo(ReadingStatus.RETURNING);
    }

    @Test
    void partnerReviewingTransitionStoresStartedAt() {
        MatchedMember member = MatchedMember.builder()
                .readingStatus(ReadingStatus.RETURNING)
                .build();
        Instant changedAt = Instant.parse("2026-06-20T05:00:00Z");

        member.updateReadingStatus(ReadingStatus.PARTNER_REVIEWING, changedAt);

        assertThat(member.getReadingStatus()).isEqualTo(ReadingStatus.PARTNER_REVIEWING);
        assertThat(member.getPartnerReviewingStartedAt()).isEqualTo(changedAt);
    }
}
