package com.example.bookiibookii.domain.push.sender;

import com.example.bookiibookii.domain.push.dto.PushMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "test"})
public class NoOpPushSender implements PushSender {

    @Override
    public void send(String deviceToken, PushMessage message) {
        log.info(
                "NoOp push. tokenSuffix={}, title={}, body={}, data={}",
                tokenSuffix(deviceToken),
                message.title(),
                message.body(),
                message.data()
        );
    }

    private String tokenSuffix(String token) {
        if (token == null || token.length() <= 8) {
            return "********";
        }
        return "..." + token.substring(token.length() - 8);
    }
}
