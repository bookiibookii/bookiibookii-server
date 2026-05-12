package com.example.bookiibookii.domain.groupbook.repository;

import com.example.bookiibookii.domain.groupbook.entity.CardImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardImageRepository extends JpaRepository<CardImage, Long> {
    Optional<CardImage> findByCard_Id(Long cardId);
    boolean existsByS3Key(String s3Key);
    boolean existsByS3KeyAndCard_IdNot(String s3Key, Long cardId);
    boolean existsByCard_Id(Long cardId);
}
