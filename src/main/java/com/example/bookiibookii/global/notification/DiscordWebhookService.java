package com.example.bookiibookii.global.notification;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.auth.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordWebhookService {

    private static final int DISCORD_CONTENT_LIMIT = 1900;
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("\\b\\d{2,3}[-. ]?\\d{3,4}[-. ]?\\d{4}\\b");
    private static final Pattern SENSITIVE_KEY_VALUE_PATTERN = Pattern.compile(
            "(?i)(address|phone|receiver|recipient|recipientName|trackingNumber|invoiceNo|waybill|운송장번호|운송장|주소|전화번호|수령인)\\s*[:=]\\s*[^,\\n\\r}]+"
    );

    private final DiscordWebhookProperties properties;
    private final RestClient discordWebhookRestClient;

    public void sendSchedulerResult(String schedulerName, int total, int success, int fail, long elapsedMs) {
        send("""
                [Scheduler 완료] %s
                처리 대상: %d건 | 성공: %d건 | 실패: %d건
                실행 시간: %dms
                """.formatted(schedulerName, total, success, fail, elapsedMs));
    }

    public void sendSchedulerError(String schedulerName, Exception exception) {
        send("""
                [Scheduler 전체 실패] %s
                exception: %s
                message: %s
                """.formatted(
                schedulerName,
                exception.getClass().getName(),
                exception.getMessage() == null ? "(empty)" : exception.getMessage()
        ));
    }

    public void sendUnexpectedExceptionAlert(HttpServletRequest request, Exception exception) {
        if (!properties.enabled() || properties.url() == null || properties.url().isBlank()) {
            return;
        }

        try {
            discordWebhookRestClient.post()
                    .uri(properties.url())
                    .body(new DiscordWebhookRequest(createMessage(request, exception)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // 웹훅 실패가 500 으로 이어지지 않도록 삼킴
            log.warn("Discord webhook alert failed", e);
        }
    }

    private void send(String content) {
        if (!properties.enabled() || properties.url() == null || properties.url().isBlank()) {
            return;
        }

        try {
            discordWebhookRestClient.post()
                    .uri(properties.url())
                    .body(new DiscordWebhookRequest(truncate(content)))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Discord webhook alert failed", e);
        }
    }

    String createMessage(HttpServletRequest request, Exception exception) {
        String groupId = extractGroupId(request).map(String::valueOf).orElse("unknown");
        String userId = extractUserId().map(String::valueOf).orElse("anonymous");
        String message = sanitize(exception.getMessage());
        String uri = sanitize(request.getRequestURI());

        String content = """
                [Unexpected Server Error]
                method: %s
                uri: %s
                exception: %s
                message: %s
                userId: %s
                groupId: %s
                """.formatted(
                request.getMethod(),
                uri == null || uri.isBlank() ? "unknown" : uri,
                exception.getClass().getName(),
                message == null || message.isBlank() ? "(empty)" : message,
                userId,
                groupId
        );

        return truncate(content);
    }

    private Optional<Long> extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return Optional.ofNullable(customUserDetails.getUser())
                    .map(User::getId);
        }
        if (principal instanceof User user) {
            return Optional.ofNullable(user.getId());
        }

        return Optional.empty();
    }

    private Optional<Long> extractGroupId(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (!(attribute instanceof Map<?, ?> pathVariables)) {
            return Optional.empty();
        }

        Object groupId = pathVariables.get("groupId");
        if (groupId == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(Long.parseLong(groupId.toString()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return null;
        }

        String masked = PHONE_NUMBER_PATTERN.matcher(value).replaceAll("[masked-phone]");
        return SENSITIVE_KEY_VALUE_PATTERN.matcher(masked).replaceAll("$1=[masked]");
    }

    private String truncate(String content) {
        if (content.length() <= DISCORD_CONTENT_LIMIT) {
            return content;
        }
        return content.substring(0, DISCORD_CONTENT_LIMIT) + "...(truncated)";
    }
}
