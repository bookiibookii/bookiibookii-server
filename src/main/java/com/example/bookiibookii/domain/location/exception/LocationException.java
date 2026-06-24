package com.example.bookiibookii.domain.location.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class LocationException extends GeneralException {
    public LocationException(BaseCode code) {
        super(code);
    }
}
