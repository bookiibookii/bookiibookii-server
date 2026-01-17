package com.example.bookiibookii.domain.group.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class GroupException extends GeneralException {
    public GroupException(BaseCode code) {
        super(code);
    }
}
