package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.global.aws.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CardImageS3Service {

    private final S3Presigner s3Presigner;
    private final AwsS3Properties awsS3Properties;

    // Presigned PUT URL 생성 (업로드용)
    public PresignedUrlResponseDTO generatePresignedPutUrl(Long cardId, int expirationMinutes) {
        String uuid = UUID.randomUUID().toString();
        String s3Key = String.format("image/cards/%d/%s", cardId, uuid);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsS3Properties.bucketName())
                .key(s3Key)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(
                presigner -> presigner
                        .signatureDuration(Duration.ofMinutes(expirationMinutes))
                        .putObjectRequest(putObjectRequest)
        );

        return PresignedUrlResponseDTO.builder()
                .s3Key(s3Key)
                .presignedUrl(presignedRequest.url().toString())
                .build();
    }

    // Presigned GET URL 생성 (조회용)

    public String generatePresignedGetUrl(String s3Key, int expirationMinutes) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(awsS3Properties.bucketName())
                .key(s3Key)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(
                presigner -> presigner
                        .signatureDuration(Duration.ofMinutes(expirationMinutes))
                        .getObjectRequest(getObjectRequest)
        );

        return presignedRequest.url().toString();
    }
}
