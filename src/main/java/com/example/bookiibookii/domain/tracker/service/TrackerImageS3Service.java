package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.tracker.exception.TrackerImageException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerImageErrorCode;
import com.example.bookiibookii.domain.groupbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.global.aws.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrackerImageS3Service {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final AwsS3Properties awsS3Properties;

    /**
     * 트래커 인증 이미지 업로드용 Presigned PUT URL 발급.
     * s3Key 형식: image/trackers/{uuid}
     */
    public PresignedUrlResponseDTO generatePresignedPutUrl(int expirationMinutes) {
        String uuid = UUID.randomUUID().toString();
        String s3Key = String.format("image/trackers/%s", uuid);

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
                .presignedPutUrl(presignedRequest.url().toString())
                .build();
    }

    /**
     * S3에 이미지가 존재하는지 확인 (HEAD 요청)
     */
    public boolean doesImageExist(String s3Key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(awsS3Properties.bucketName())
                    .key(s3Key)
                    .build();
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            log.error("S3 접근 오류 (s3Key: {}): {}", s3Key, e.getMessage(), e);
            throw new TrackerImageException(TrackerImageErrorCode.S3_ACCESS_ERROR);
        } catch (SdkClientException e) {
            log.error("S3 클라이언트 오류 (s3Key: {}): {}", s3Key, e.getMessage(), e);
            throw new TrackerImageException(TrackerImageErrorCode.S3_ACCESS_ERROR);
        }
    }

    /**
     * 인증 이미지 조회용 Presigned GET URL 발급.
     */
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
