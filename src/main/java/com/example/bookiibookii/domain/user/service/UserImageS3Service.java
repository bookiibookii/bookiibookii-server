package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.user.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.user.exception.UserImageException;
import com.example.bookiibookii.domain.user.exception.code.UserImageErrorCode;
import com.example.bookiibookii.domain.userbook.exception.CardImageException;
import com.example.bookiibookii.global.aws.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserImageS3Service {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final AwsS3Properties awsS3Properties;

    // Presigned PUT URL 생성 (업로드용 - UUID 기반)
    public PresignedUrlResponseDTO generatePresignedPutUrl(Long userId, int expirationMinutes) {
        String uuid = UUID.randomUUID().toString();
        String s3Key = String.format("image/users/%d/%s", userId, uuid);

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

    /**
     * S3에 이미지가 존재하는지 확인 (HEAD 요청)
     *
     * @param s3Key S3 키
     * @return 이미지가 존재하면 true, 존재하지 않으면 false
     * @throws UserImageException S3 접근 오류 시 (권한 오류, 네트워크 오류 등)
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
            log.error("S3 접근 오류 (s3Key: {}): statusCode={}, errorCode={}", s3Key, e.statusCode(), e.awsErrorDetails() != null ? e.awsErrorDetails().errorCode() : "N/A", e);
            throw new UserImageException(UserImageErrorCode.S3_ACCESS_ERROR);
        } catch (SdkClientException e) {
            log.error("S3 클라이언트 오류 (s3Key: {}): message={}", s3Key, e.getMessage(), e);
            throw new UserImageException(UserImageErrorCode.S3_ACCESS_ERROR);
        }
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
