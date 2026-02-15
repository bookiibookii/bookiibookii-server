package com.example.bookiibookii.domain.support.notice.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class NoticeException extends GeneralException {
    public NoticeException(BaseCode code) {
        super(code);
    }
}
