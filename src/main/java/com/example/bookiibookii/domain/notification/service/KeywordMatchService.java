package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.entity.Keyword;
import com.example.bookiibookii.domain.notification.repository.KeywordRepository;
import com.example.bookiibookii.domain.notification.util.KeywordNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class KeywordMatchService {

    private final KeywordRepository keywordRepository;

    @Transactional(readOnly = true)
    public List<Keyword> matchForBook(String bookTitle, String authorName) {
        String titleNorm = KeywordNormalizer.normalize(bookTitle);
        String authorNorm = KeywordNormalizer.normalize(authorName);

        // 완전 조회
        Set<String> exactTargets = new HashSet<>();
        if (notBlank(titleNorm)) exactTargets.add(titleNorm);
        if (notBlank(authorNorm)) exactTargets.add(authorNorm);

        Map<Long, Keyword> matched = new LinkedHashMap<>();
        if (!exactTargets.isEmpty()) {
            for (Keyword k : keywordRepository.findAllByNormalizedContentIn(exactTargets)) {
                matched.putIfAbsent(k.getId(), k);
            }
        }

        // 부분 조회
        Set<String> prefixes = new HashSet<>();
        prefixes.addAll(twoGrams(titleNorm));
        prefixes.addAll(twoGrams(authorNorm));
        if (!prefixes.isEmpty()) {
            List<Keyword> candidates = keywordRepository.findAllByPrefix2In(prefixes);

            for (Keyword k : candidates) {
                String kn = k.getNormalizedContent();
                if (kn == null || kn.length() < 2) continue;

                if ((notBlank(titleNorm) && titleNorm.contains(kn)) ||
                        (notBlank(authorNorm) && authorNorm.contains(kn))) {
                    matched.putIfAbsent(k.getId(), k);
                }
            }
        }

        return new ArrayList<>(matched.values());
    }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    private Set<String> twoGrams(String s) {
        if (s == null) return Set.of();
        if (s.length() < 2) return Set.of(); // 2-gram
        Set<String> out = new HashSet<>();
        for (int i = 0; i <= s.length() - 2; i++) out.add(s.substring(i, i + 2));
        return out;
    }
}
