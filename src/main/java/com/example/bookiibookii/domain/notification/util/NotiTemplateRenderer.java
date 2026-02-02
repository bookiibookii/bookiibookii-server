package com.example.bookiibookii.domain.notification.util;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotiTemplateRenderer {

    public String render(String template, Map<String, String> vars) {
        if (template == null) return null;

        String result = template;
        for (var entry : vars.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", safe(entry.getValue()));
        }
        return result;
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
