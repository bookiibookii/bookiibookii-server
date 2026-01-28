package com.example.bookiibookii.domain.user.repository;

import com.example.bookiibookii.domain.user.entity.UserImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserImageRepository extends JpaRepository<UserImage, Long> {
    Optional<UserImage> findByUser_Id(Long userId);
    boolean existsByS3Key(String s3Key);
    boolean existsByS3KeyAndUser_IdNot(String s3Key, Long userId);
    boolean existsByUser_Id(Long userId);
}
