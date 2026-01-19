package com.example.bookiibookii.domain.tag.cache;

import com.example.bookiibookii.domain.tag.entity.Tag;
import com.example.bookiibookii.domain.tag.enums.TagCode;
import com.example.bookiibookii.domain.tag.enums.TagType;
import com.example.bookiibookii.domain.tag.repository.TagRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TagCache {
    private final TagRepository tagRepository;

    private Map<TagCode, Tag> tagCache;

    // 애플리케이션 시작 시 Tag 정보를 모두 로딩
    @PostConstruct
    public void load() {
        this.tagCache = tagRepository.findAll()
                .stream()
                .collect(Collectors.toUnmodifiableMap(
                        Tag::getCode,
                        Function.identity()
                ));
        validateTagCodes(); //  TagCode Enum ↔ DB 정합성 검증
    }

    // 누락된 태그 검증
    private void validateTagCodes() {
        // Enum에 정의된 TagCode는 반드시 DB에 존재해야 함
        Set<TagCode> missing = EnumSet.allOf(TagCode.class)
                .stream()
                .filter(code -> !tagCache.containsKey(code))
                .collect(Collectors.toSet());

        if (!missing.isEmpty()) {
            throw new IllegalStateException("DB에 없는 TagCode: " + missing);
        }
    }

    // TagCode로 Tag 조회
    public Tag get(TagCode code) {
        Tag tag = tagCache.get(code);
        if (tag == null) {
            throw new IllegalArgumentException("유효하지 않은 태그코드: " + code);
        }
        return tag;
    }

    // 특정 TagType에 해당하는 태그 목록 조회
    public List<Tag> getByType(TagType type) {
        return tagCache.values().stream()
                .filter(tag -> tag.getType() == type)
                .toList();
    }
}
