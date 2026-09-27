package com.example.bookiibookii.domain.group.repository;

import com.example.bookiibookii.domain.group.entity.PopularBook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopularBookRepository extends JpaRepository<PopularBook, Long> {
}
