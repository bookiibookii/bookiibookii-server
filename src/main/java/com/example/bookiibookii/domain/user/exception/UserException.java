package com.example.bookiibookii.domain.user.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class UserException extends GeneralException {
    public UserException(BaseCode code){
        super(code);
    }
}
