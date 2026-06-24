package com.example.bookiibookii.domain.tracker.exception;

import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class TrackerException extends GeneralException {
    public TrackerException(TrackerErrorCode errorCode) {
        super(errorCode);
    }
}
