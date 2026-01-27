package com.example.bookiibookii.domain.aladin.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class AladinException extends GeneralException {
    public AladinException(BaseCode code) {
        super(code);
    }
}
