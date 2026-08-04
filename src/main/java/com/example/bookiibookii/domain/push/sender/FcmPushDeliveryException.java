package com.example.bookiibookii.domain.push.sender;

import com.google.firebase.messaging.MessagingErrorCode;
import lombok.Getter;

@Getter
public class FcmPushDeliveryException extends RuntimeException {

    private final MessagingErrorCode errorCode;

    public FcmPushDeliveryException(
            MessagingErrorCode errorCode,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
