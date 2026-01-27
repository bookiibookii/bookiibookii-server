package com.example.bookiibookii.domain.comment.exception;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class CommentException extends GeneralException {
    public CommentException(BaseCode code){
        super(code);
    }
}
