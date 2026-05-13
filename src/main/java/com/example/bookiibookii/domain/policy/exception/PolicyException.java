package com.example.bookiibookii.domain.policy.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class PolicyException extends GeneralException {
    public PolicyException(BaseCode code){
        super(code);
    }
}
