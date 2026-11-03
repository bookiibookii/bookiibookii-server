package com.example.bookiibookii.domain.nlgoKr.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class SeojiException extends GeneralException {

    public SeojiException(BaseCode code) {
        super(code);
    }

    public SeojiException(BaseCode code, Throwable cause) {
        super(code);
        initCause(cause);
    }
}
