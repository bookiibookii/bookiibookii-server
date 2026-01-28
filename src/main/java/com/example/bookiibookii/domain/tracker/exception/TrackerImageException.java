package com.example.bookiibookii.domain.tracker.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class TrackerImageException extends GeneralException {
    public TrackerImageException(BaseCode code) {
        super(code);
    }
}
