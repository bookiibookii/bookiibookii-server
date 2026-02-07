package com.example.bookiibookii.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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
    private final ObjectMapper objectMapper; // Spring이 기본 제공하는 ObjectMapper 주입
    private static final String RANKING_KEY_PREFIX = "search:ranking:";
    private static final String COMBINED_KEY = "search:ranking:combined";

    // 데이터 저장 (객체를 받아서 JSON String으로 변환 후 저장)
    public void set(String key, Object data, int minutes) {
        try {
            // Object -> JSON String 변환
            String jsonValue = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, jsonValue, minutes, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("Redis 저장 중 JSON 변환 에러: {}", e.getMessage());
            throw new RuntimeException("Redis Parsing Error");
        }
    }

    // 데이터 조회 (JSON String을 가져와서 원하는 클래스 타입으로 변환)
    public <T> T get(String key, Class<T> classType) {
        String jsonValue = redisTemplate.opsForValue().get(key);

        if (jsonValue == null) {
            return null;
        }

        try {
            // JSON String -> Object 변환
            return objectMapper.readValue(jsonValue, classType);
        } catch (JsonProcessingException e) {
            log.error("Redis 조회 중 JSON 변환 에러: {}", e.getMessage());
            throw new RuntimeException("Redis Parsing Error");
        }
    }

    // 데이터 삭제
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    // 존재 여부
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // Blacklist 등 유효기간을 밀리초 단위로 설정해야 할 때 사용
    public void setBlackList(String key, Object data, Long milliSeconds) {
        try {
            String jsonValue = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, jsonValue, milliSeconds, TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException e) {
            log.error("Redis 저장 에러: {}", e.getMessage());
            throw new RuntimeException("Redis Parsing Error", e);
        }
    }

    //인기검색어
    public void incrementSearchScore(String keyword) {
        if (keyword == null || keyword.isBlank()) return;

        String cleanKeyword = keyword.trim();
        String todayKey = RANKING_KEY_PREFIX + LocalDate.now();

        // 오늘자 키에 점수 1 증가
        redisTemplate.opsForZSet().incrementScore(todayKey, cleanKeyword, 1);

        // 해당 날짜 키에 90일 TTL 설정 (자동 삭제)
        redisTemplate.expire(todayKey, 90, TimeUnit.DAYS);

        // log.info("인기 검색어 기록 (날짜별): {} -> {}", todayKey, cleanKeyword);
    }

    //인기 검색어 조회
    public List<String> getTopKeywords(int limit) {
        // 합산된 결과(캐시)가 없으면 새로 생성
        if (Boolean.FALSE.equals(redisTemplate.hasKey(COMBINED_KEY))) {
            List<String> last90DaysKeys = IntStream.range(0, 90)
                    .mapToObj(i -> RANKING_KEY_PREFIX + LocalDate.now().minusDays(i))
                    .filter(key -> Boolean.TRUE.equals(redisTemplate.hasKey(key)))
                    .collect(Collectors.toList());

            if (last90DaysKeys.isEmpty()) return new ArrayList<>();

            // Redis 자체 기능으로 90개 키의 점수를 모두 합산하여 COMBINED_KEY에 저장
            redisTemplate.opsForZSet().unionAndStore(last90DaysKeys.get(0),
                    last90DaysKeys.subList(1, last90DaysKeys.size()),
                    COMBINED_KEY);

            // 합산 결과는 10분간 유지 (성능 최적화)
            redisTemplate.expire(COMBINED_KEY, 10, TimeUnit.MINUTES);
        }

        Set<String> range = redisTemplate.opsForZSet().reverseRange(COMBINED_KEY, 0, limit - 1);
        return range == null ? new ArrayList<>() : new ArrayList<>(range);
    }
}
