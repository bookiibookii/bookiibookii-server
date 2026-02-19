package com.example.bookiibookii.domain.support.report.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportType {
    ABUSE("욕설/비방"),
    SPAM("스팸/광고"),
    NO_SHOW("책 미발송/노쇼/연락두절"),
    DAMAGED_BOOK("책 파손/낙서");

    private final String description;
}
