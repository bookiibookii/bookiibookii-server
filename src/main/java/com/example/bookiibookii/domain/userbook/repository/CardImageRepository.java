package com.example.bookiibookii.domain.userbook.repository;

import com.example.bookiibookii.domain.userbook.entity.CardImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardImageRepository extends JpaRepository<CardImage, Long> {
    List<CardImage> findAllByCard_IdOrderByIdAsc(Long cardId);
    boolean existsByS3Key(String s3Key);
}
