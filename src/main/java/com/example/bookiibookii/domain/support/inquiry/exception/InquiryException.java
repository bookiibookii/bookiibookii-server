package com.example.bookiibookii.domain.support.inquiry.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class InquiryException extends GeneralException {
    public InquiryException(BaseCode code) {
        super(code);
    }
}
