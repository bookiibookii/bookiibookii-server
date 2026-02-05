package com.example.bookiibookii.domain.notification.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class NotificationException extends GeneralException {
    public NotificationException(BaseCode code){
        super(code);
    }
}

