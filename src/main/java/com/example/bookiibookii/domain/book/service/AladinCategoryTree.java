package com.example.bookiibookii.domain.book.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class AladinCategoryTree {

    private final Map<Long, Long> parentCidMap = new HashMap<>();

    @PostConstruct
    public void load() {
        try {
            ClassPathResource resource = new ClassPathResource("aladin-category-tree-compact.csv");

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                boolean firstLine = true;

                while ((line = br.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }

                    String[] parts = line.split(",", -1);

                    if (parts.length < 2) {
                        continue;
                    }

                    Long cid = parseLong(parts[0]);
                    Long parentCid = parseLong(parts[1]);

                    if (cid != null && parentCid != null) {
                        parentCidMap.put(cid, parentCid);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to initialize Aladin category tree", e);
        }
    }

    public Optional<Long> getParentCid(Long cid) {
        return Optional.ofNullable(parentCidMap.get(cid));
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return Long.parseLong(value.trim());
    }
}