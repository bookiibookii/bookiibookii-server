package com.example.bookiibookii.domain.group.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class MatchedMemberException extends GeneralException {
    public MatchedMemberException(BaseCode code) {
        super(code);
    }
}
