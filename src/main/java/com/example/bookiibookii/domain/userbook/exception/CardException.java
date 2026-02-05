package com.example.bookiibookii.domain.userbook.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class CardException extends GeneralException {
    public CardException(BaseCode code) {
        super(code);
    }
}
