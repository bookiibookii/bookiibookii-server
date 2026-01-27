package com.example.bookiibookii.domain.notification.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class KeywordException extends GeneralException {
    public KeywordException(BaseCode code){
        super(code);
    }
}

