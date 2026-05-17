package com.example.bookiibookii.domain.aladin.repository;

import com.example.bookiibookii.domain.aladin.entity.BestsellerIsbn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BestsellerIsbnRepository extends JpaRepository<BestsellerIsbn, Long> {

    @Query("SELECT b.isbn13 FROM BestsellerIsbn b ORDER BY b.rank ASC")
    List<String> findAllIsbn13OrderByRank();
}
