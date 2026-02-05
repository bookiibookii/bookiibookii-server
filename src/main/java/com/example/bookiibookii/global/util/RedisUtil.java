package com.example.bookiibookii.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper; // Spring이 기본 제공하는 ObjectMapper 주입
    private static final String SEARCH_RANKING_KEY = "search:ranking";

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
    //검색어 점수 1증가
    public void incrementSearchScore(String keyword) {
        if (keyword == null || keyword.isBlank()) return;

        // 검색어 정규화 (공백 제거 및 필요시 소문자 변환)
        String cleanKeyword = keyword.trim();

        // ZSET의 score를 1 증가시킴
        redisTemplate.opsForZSet().incrementScore(SEARCH_RANKING_KEY, cleanKeyword, 1);
        log.info("인기 검색어 카운팅 추가: {}", cleanKeyword);
    }

    //인기 검색어 조회
    public List<String> getTopKeywords(int limit) {
        // 역순(큰 점수 순)으로 상위 N개 멤버 가져오기
        Set<String> range = redisTemplate.opsForZSet().reverseRange(SEARCH_RANKING_KEY, 0, limit - 1);

        if (range == null || range.isEmpty()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(range);
    }
}
