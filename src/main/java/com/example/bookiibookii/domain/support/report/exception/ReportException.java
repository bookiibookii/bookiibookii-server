package com.example.bookiibookii.domain.support.report.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class ReportException extends GeneralException {
    public ReportException(BaseCode code) {
        super(code);
    }
}
