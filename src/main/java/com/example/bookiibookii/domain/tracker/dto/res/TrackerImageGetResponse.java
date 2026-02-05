package com.example.bookiibookii.domain.tracker.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrackerImageGetResponse {
    /** 인증 이미지 조회용 Presigned GET URL */
    private String presignedGetUrl;
}
