package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileImageUrlResolver {

    private static final int PRESIGNED_GET_URL_EXPIRATION_MINUTES = 60;

    private final UserImageS3Service userImageS3Service;

    public String resolve(User user) {
        if (user == null || user.getUserImage() == null) {
            return null;
        }

        return userImageS3Service.generatePresignedGetUrl(
                user.getUserImage().getS3Key(),
                PRESIGNED_GET_URL_EXPIRATION_MINUTES
        );
    }
}
