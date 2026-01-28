package com.example.bookiibookii.domain.userbook.service;

import com.example.bookiibookii.domain.userbook.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.global.aws.AwsS3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
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
    private final S3Client s3Client;
    private final AwsS3Properties awsS3Properties;

    // Presigned PUT URL 생성 (업로드용 - UUID 기반)
    public PresignedUrlResponseDTO generatePresignedPutUrl(int expirationMinutes) {
        String uuid = UUID.randomUUID().toString();
        String s3Key = String.format("image/cards/%s", uuid);

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

    // S3에 이미지가 존재하는지 확인 (HEAD 요청)
    public boolean doesImageExist(String s3Key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(awsS3Properties.bucketName())
                    .key(s3Key)
                    .build();
            
            s3Client.headObject(headRequest);
            return true;
        } catch (NoSuchKeyException e) {
            // 이미지가 존재하지 않음
            return false;
        } catch (Exception e) {
            // 기타 예외 (권한 문제, 네트워크 문제 등) 발생 시 로그 출력 후 false 반환
            // 또는 예외를 다시 던질 수도 있음
            System.err.println("S3 HEAD 요청 중 예외 발생: " + e.getMessage());
            e.printStackTrace();
            return false;
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
