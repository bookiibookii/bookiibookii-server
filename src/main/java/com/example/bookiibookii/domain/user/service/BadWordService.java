package com.example.bookiibookii.domain.user.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BadWordService {

    private List<String> badWordList = new ArrayList<>();

    @PostConstruct
    public void init() {
        // 서버 시작 시 파일에서 읽어와 메모리에 적재
        try {
            ClassPathResource resource = new ClassPathResource("badword.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            this.badWordList = reader.lines()
                    .map(String::trim)
                    .filter(word -> !word.isEmpty()) // 공백 라인 제거
                    .collect(Collectors.toList());

        } catch (IOException e) {
        }
    }

    public boolean containsBadWord(String input) {
        if (input == null || badWordList.isEmpty()) {
            return false;
        }
        for (String badWord : badWordList) {
            if (input.contains(badWord)) {
                return true;
            }
        }
        return false;
    }
}