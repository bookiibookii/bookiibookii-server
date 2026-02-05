package com.example.bookiibookii.domain.review.exception;

import com.example.bookiibookii.domain.review.exception.code.ReviewErrorCode;
import com.example.bookiibookii.global.apiPayload.exception.GeneralException;

public class ReviewException extends GeneralException {
    public ReviewException(ReviewErrorCode errorCode) {
        super(errorCode);
    }
}
