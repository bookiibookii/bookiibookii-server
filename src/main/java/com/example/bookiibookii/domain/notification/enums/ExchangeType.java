package com.example.bookiibookii.domain.notification.enums;

import com.example.bookiibookii.domain.group.enums.TradeType;

public enum ExchangeType {
    DIRECT,
    DELIVERY;

    public static ExchangeType from(TradeType tradeType) {
        if (tradeType == null) {
            return null;
        }
        return valueOf(tradeType.name());
    }
}
