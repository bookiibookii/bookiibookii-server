package com.example.bookiibookii.domain.tag.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class TagException extends GeneralException {
    public TagException(BaseCode code){
        super(code);
    }
}
