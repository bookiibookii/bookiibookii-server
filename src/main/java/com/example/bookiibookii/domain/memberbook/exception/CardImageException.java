package com.example.bookiibookii.domain.memberbook.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class CardImageException extends GeneralException {
    public CardImageException(BaseCode code) {
        super(code);
    }
}
