package com.example.bookiibookii.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.bookiibookii.global.time.TimeUtils;
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
    private final ObjectMapper objectMapper; // Spring이 기본 제공하는 ObjectMapper 주입

    // yml에 설정된 prefix 주입 (기본값 빈 문자열)
    @Value("${spring.data.redis.prefix:}")
    private String prefix;

    private static final String RANKING_KEY_PREFIX = "search:ranking:";
    private static final String COMBINED_KEY = "search:ranking:combined";

    // 공통 Prefix 적용 메서드
    private String applyPrefix(String key) {
        return (prefix == null ? "" : prefix) + key;
    }

    // 데이터 저장 (객체를 받아서 JSON String으로 변환 후 저장)
    public void set(String key, Object data, int minutes) {
        try {
            // Object -> JSON String 변환
            String jsonValue = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(applyPrefix(key), jsonValue, minutes, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("Redis 저장 중 JSON 변환 에러: {}", e.getMessage());
            throw new RuntimeException("Redis Parsing Error");
        } catch (Exception e) {
            log.error("[SET] Redis 연결 에러 : {}", e.getMessage());
        }
    }

    // 데이터 조회 (JSON String을 가져와서 원하는 클래스 타입으로 변환)
    public <T> T get(String key, Class<T> classType) {
        try {
            String jsonValue = redisTemplate.opsForValue().get(applyPrefix(key));
            if (jsonValue == null) return null;
            // JSON String -> Object 변환
            return objectMapper.readValue(jsonValue, classType);
        } catch (JsonProcessingException e) {
            log.error("Redis 조회 중 JSON 변환 에러: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("[GET] Redis 연결 에러 : {}", e.getMessage());
            return null;
        }
    }

    // 데이터 삭제
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(applyPrefix(key)));
        } catch (Exception e) {
            log.error("[DELETE] Redis 연결 에러 : {}", e.getMessage());
            return false;
        }
    }

    // 존재 여부
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(applyPrefix(key)));
        } catch (Exception e) {
            log.error("[HAS_KEY] Redis 연결 에러 : {}", e.getMessage());
            return false;
        }
    }

    // Blacklist 등 유효기간을 밀리초 단위로 설정해야 할 때 사용
    public void setBlackList(String key, Object data, Long milliSeconds) {
        try {
            String jsonValue = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(applyPrefix(key), jsonValue, milliSeconds, TimeUnit.MILLISECONDS);
        } catch (JsonProcessingException e) {
            log.error("Redis 저장 에러: {}", e.getMessage());
            throw new RuntimeException("Redis Parsing Error", e);
        } catch (Exception e) {
            log.error("[BLACKLIST] Redis 연결 에러 : {}", e.getMessage());
        }
    }

    //인기검색어
    public void incrementSearchScore(String keyword) {
        if (keyword == null || keyword.isBlank()) return;

        try {
            String cleanKeyword = keyword.trim();
            // TODO: 검색어 랭킹 기준일도 Clock 주입으로 테스트 가능하게 정리한다.
            String todayKey = RANKING_KEY_PREFIX + TimeUtils.todayKst();
            String prefixedKey = applyPrefix(todayKey);

            // 오늘자 키에 점수 1 증가
            redisTemplate.opsForZSet().incrementScore(prefixedKey, cleanKeyword, 1);

            // 해당 날짜 키에 90일 TTL 설정 (자동 삭제)
            redisTemplate.expire(prefixedKey, 90, TimeUnit.DAYS);

            // log.info("인기 검색어 기록 (날짜별): {} -> {}", todayKey, cleanKeyword);
        } catch (Exception e) {
            log.error("[INCREMENT] Redis 연결 에러 : {}", e.getMessage());
        }
    }

    //인기 검색어 조회
    public List<String> getTopKeywords(int limit) {
        try {
            String combinedKeyWithPrefix = applyPrefix(COMBINED_KEY);

            // 합산된 결과(캐시)가 없으면 새로 생성
            if (Boolean.FALSE.equals(redisTemplate.hasKey(combinedKeyWithPrefix))) {
                // TODO: 검색어 랭킹 기준일도 Clock 주입으로 테스트 가능하게 정리한다.
                List<String> last90DaysKeys = IntStream.range(0, 90)
                        .mapToObj(i -> applyPrefix(RANKING_KEY_PREFIX + TimeUtils.todayKst().minusDays(i)))
                        .filter(key -> Boolean.TRUE.equals(redisTemplate.hasKey(key)))
                        .collect(Collectors.toList());

                if (last90DaysKeys.isEmpty()) return new ArrayList<>();

                // Redis 자체 기능으로 90개 키의 점수를 모두 합산하여 COMBINED_KEY에 저장
                redisTemplate.opsForZSet().unionAndStore(last90DaysKeys.get(0),
                        last90DaysKeys.subList(1, last90DaysKeys.size()),
                        combinedKeyWithPrefix);

                // 합산 결과는 10분간 유지 (성능 최적화)
                redisTemplate.expire(combinedKeyWithPrefix, 10, TimeUnit.MINUTES);
            }

            Set<String> range = redisTemplate.opsForZSet().reverseRange(combinedKeyWithPrefix, 0, limit - 1);
            return range == null ? new ArrayList<>() : new ArrayList<>(range);
        } catch (Exception e) {
            log.error("[GET_TOP_KEYWORDS] Redis 연결 에러 : {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
