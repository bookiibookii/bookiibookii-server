package com.example.bookiibookii.global.util;

import com.example.bookiibookii.global.notification.DiscordWebhookService;
import com.example.bookiibookii.global.time.TimeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final DiscordWebhookService discordWebhookService;

    @Value("${spring.data.redis.prefix:}")
    private String prefix;

    private static final String RANKING_KEY_PREFIX = "search:ranking:";
    private static final String COMBINED_KEY = "search:ranking:combined";

    // 연속 Redis 장애 시 Discord 알림 폭주 방지 (1분 쿨다운)
    private static final long ALERT_COOLDOWN_MS = 60_000L;
    private volatile long lastRedisAlertTime = 0;

    private String applyPrefix(String key) {
        return (prefix == null ? "" : prefix) + key;
    }

    private void sendRedisAlertIfNeeded(String operation, Exception e) {
        long now = System.currentTimeMillis();
        if (now - lastRedisAlertTime >= ALERT_COOLDOWN_MS) {
            lastRedisAlertTime = now;
            discordWebhookService.sendRedisErrorAlert(operation, e);
        }
    }

    public void set(String key, Object data, int minutes) {
        try {
            String jsonValue = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(applyPrefix(key), jsonValue, minutes, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("Redis 저장 중 JSON 변환 에러: {}", e.getMessage());
            throw new RuntimeException("Redis Parsing Error");
        } catch (Exception e) {
            log.error("[SET] Redis 연결 에러 : {}", e.getMessage());
            sendRedisAlertIfNeeded("SET", e);
            throw new RuntimeException("Redis SET 실패", e);
        }
    }

    public <T> T get(String key, Class<T> classType) {
        try {
            String jsonValue = redisTemplate.opsForValue().get(applyPrefix(key));
            if (jsonValue == null) return null;
            return objectMapper.readValue(jsonValue, classType);
        } catch (JsonProcessingException e) {
            log.error("Redis 조회 중 JSON 변환 에러: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[GET] Redis 연결 에러 : {}", e.getMessage());
            sendRedisAlertIfNeeded("GET", e);
            throw new RuntimeException("Redis GET 실패", e);
        }
    }

    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(applyPrefix(key)));
        } catch (Exception e) {
            log.error("[DELETE] Redis 연결 에러 : {}", e.getMessage());
            sendRedisAlertIfNeeded("DELETE", e);
            return false;
        }
    }

    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(applyPrefix(key)));
        } catch (Exception e) {
            log.error("[HAS_KEY] Redis 연결 에러 : {}", e.getMessage());
            sendRedisAlertIfNeeded("HAS_KEY", e);
            return false;
        }
    }

    public void setBlackList(String key, Object data, Long milliSeconds) {
        try {
            String jsonValue = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(applyPrefix(key), jsonValue, milliSeconds, TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException e) {
            log.error("Redis 저장 에러: {}", e.getMessage());
            throw new RuntimeException("Redis Parsing Error", e);
        } catch (Exception e) {
            log.error("[BLACKLIST] Redis 연결 에러 : {}", e.getMessage());
            sendRedisAlertIfNeeded("BLACKLIST", e);
        }
    }

    public void incrementSearchScore(String keyword) {
        if (keyword == null || keyword.isBlank()) return;

        try {
            String cleanKeyword = keyword.trim();
            // TODO: 검색어 랭킹 기준일도 Clock 주입으로 테스트 가능하게 정리한다.
            String todayKey = RANKING_KEY_PREFIX + TimeUtils.todayKst();
            String prefixedKey = applyPrefix(todayKey);

            redisTemplate.opsForZSet().incrementScore(prefixedKey, cleanKeyword, 1);
            redisTemplate.expire(prefixedKey, 90, TimeUnit.DAYS);
        } catch (Exception e) {
            log.error("[INCREMENT] Redis 연결 에러 : {}", e.getMessage());
            sendRedisAlertIfNeeded("INCREMENT", e);
        }
    }

    public List<String> getTopKeywords(int limit) {
        try {
            String combinedKeyWithPrefix = applyPrefix(COMBINED_KEY);

            if (Boolean.FALSE.equals(redisTemplate.hasKey(combinedKeyWithPrefix))) {
                // TODO: 검색어 랭킹 기준일도 Clock 주입으로 테스트 가능하게 정리한다.
                List<String> last90DaysKeys = IntStream.range(0, 90)
                        .mapToObj(i -> applyPrefix(RANKING_KEY_PREFIX + TimeUtils.todayKst().minusDays(i)))
                        .filter(key -> Boolean.TRUE.equals(redisTemplate.hasKey(key)))
                        .collect(Collectors.toList());

                if (last90DaysKeys.isEmpty()) return new ArrayList<>();

                redisTemplate.opsForZSet().unionAndStore(last90DaysKeys.get(0),
                        last90DaysKeys.subList(1, last90DaysKeys.size()),
                        combinedKeyWithPrefix);

                redisTemplate.expire(combinedKeyWithPrefix, 10, TimeUnit.MINUTES);
            }

            Set<String> range = redisTemplate.opsForZSet().reverseRange(combinedKeyWithPrefix, 0, limit - 1);
            return range == null ? new ArrayList<>() : new ArrayList<>(range);
        } catch (Exception e) {
            log.error("[GET_TOP_KEYWORDS] Redis 연결 에러 : {}", e.getMessage());
            sendRedisAlertIfNeeded("GET_TOP_KEYWORDS", e);
            return new ArrayList<>();
        }
    }
}
