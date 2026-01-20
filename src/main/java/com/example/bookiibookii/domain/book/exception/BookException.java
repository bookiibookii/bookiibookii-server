package com.example.bookiibookii.domain.book.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class BookException extends GeneralException {
    public BookException(BaseCode code) {
        super(code);
    }
}
