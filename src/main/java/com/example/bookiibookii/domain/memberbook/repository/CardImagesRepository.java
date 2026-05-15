package com.example.bookiibookii.domain.memberbook.repository;

import com.example.bookiibookii.domain.memberbook.entity.CardImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardImagesRepository extends JpaRepository<CardImages, Long> {

    boolean existsByS3Key(String s3Key);
}
