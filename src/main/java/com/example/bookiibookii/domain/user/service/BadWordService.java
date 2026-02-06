package com.example.bookiibookii.domain.user.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Trie;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadWordService {

    private Trie trie;

    @PostConstruct
    public void init() {
        try {
            // 파일에서 금칙어 읽기
            ClassPathResource resource = new ClassPathResource("badword.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            List<String> badWords = reader.lines()
                    .map(String::trim)
                    .filter(word -> !word.isEmpty())
                    .collect(Collectors.toList());

            // 아호-코라식 Trie 구축
            this.trie = Trie.builder()
                    .ignoreCase()  // 영어 대소문자 무시 (Shit == shit)
                    .ignoreOverlaps() // 중복 매칭 최적화
                    .addKeywords(badWords)
                    .build();

            log.info("금칙어 Trie 구축 완료: 총 {}개 단어 로드됨", badWords.size());

        } catch (IOException e) {
            log.error("[CRITICAL] 금칙어 파일(badwords.txt) 로드 실패", e);
            throw new RuntimeException("Fail to load badwords.txt", e);
        }
    }

    // 금칙어 포함 여부 검사 => 특수문자를 제거한 뒤 Trie 알고리즘으로 고속 검색
    public boolean containsBadWord(String input) {
        if (input == null || trie == null) {
            return false;
        }

        // 변칙 패턴 방어 (Normalization)
        // 한글, 영문, 숫자만 남기고 나머지(공백, 특수문자 등) 제거
        // "바 _ 보" -> "바보" / "S.h.i.t" -> "Shit"
        String normalizedInput = input.replaceAll("[^가-힣a-zA-Z0-9]", "");

        // 정제된 문자열로 Trie 검색 (O(N) 속도)
        return trie.containsMatch(normalizedInput);
    }
}