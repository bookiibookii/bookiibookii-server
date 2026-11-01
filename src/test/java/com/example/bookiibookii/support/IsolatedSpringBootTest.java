package com.example.bookiibookii.support;

import com.example.bookiibookii.domain.aladin.config.AladinClient;
import com.example.bookiibookii.global.auth.social.AppleAuthClient;
import com.example.bookiibookii.global.auth.social.AppleTokenVerifier;
import com.example.bookiibookii.global.auth.social.GoogleTokenVerifier;
import com.example.bookiibookii.global.auth.social.KakaoTokenVerifier;
import com.example.bookiibookii.global.util.RedisUtil;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Full application context with real application/JPA beans and mocked external boundaries. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@ActiveProfiles("test")
@MockitoBean(types = {
        S3Client.class, S3Presigner.class,
        LettuceConnectionFactory.class, RedisUtil.class,
        AppleTokenVerifier.class, GoogleTokenVerifier.class, KakaoTokenVerifier.class,
        AppleAuthClient.class, AladinClient.class
})
// Keep @Scheduled registration validation active while preventing task execution in tests.
@MockitoBean(types = TaskScheduler.class)
public @interface IsolatedSpringBootTest {
}
