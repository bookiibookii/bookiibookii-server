package com.example.bookiibookii.domain.memberbook.repository;

import com.example.bookiibookii.domain.memberbook.entity.CardImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardImagesRepository extends JpaRepository<CardImages, Long> {

    boolean existsByS3Key(String s3Key);

    boolean existsByS3KeyAndCard_IdNot(String s3Key, Long cardId);

    Optional<CardImages> findByCard_Id(Long cardId);
}
