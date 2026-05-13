package com.example.bookiibookii.domain.groupbook.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class CardException extends GeneralException {
    public CardException(BaseCode code) {
        super(code);
    }
}
