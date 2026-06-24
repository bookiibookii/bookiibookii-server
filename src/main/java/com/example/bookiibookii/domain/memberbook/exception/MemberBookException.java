package com.example.bookiibookii.domain.memberbook.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class MemberBookException extends GeneralException {
    public MemberBookException(BaseCode code) {
        super(code);
    }
}
