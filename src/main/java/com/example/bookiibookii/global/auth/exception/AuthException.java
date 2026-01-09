package com.example.bookiibookii.global.auth.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class AuthException extends GeneralException {
    public AuthException(BaseCode code) {
        super(code);
    }
}
