package com.example.bookiibookii.domain.support.faq.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class FaqException extends GeneralException {
    public FaqException(BaseCode code) {
        super(code);
    }
}
