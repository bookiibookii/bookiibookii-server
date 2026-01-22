package com.example.bookiibookii.domain.userbook.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class UserBookException extends GeneralException {
    public UserBookException(BaseCode code){
        super(code);
    }
}
