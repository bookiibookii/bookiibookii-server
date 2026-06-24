package com.example.bookiibookii.domain.push.sender;

import com.example.bookiibookii.domain.push.dto.PushMessage;

public interface PushSender {

    void send(String deviceToken, PushMessage message);
}
