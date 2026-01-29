package com.example.bookiibookii.domain.notification.repository;

import com.example.bookiibookii.domain.notification.entity.Keyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {

    Optional<Keyword> findByNormalizedContent(String normalizedContent);

    List<Keyword> findAllByNormalizedContentIn(Collection<String> normalizedContents);

    List<Keyword> findAllByPrefix2In(Collection<String> prefixes);
}
