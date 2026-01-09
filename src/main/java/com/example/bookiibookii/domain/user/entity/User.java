package com.example.bookiibookii.domain.user.entity;

import com.example.bookiibookii.domain.user.enums.Role;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.global.auth.social.SocialUserInfo;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

import static com.example.bookiibookii.domain.user.enums.Role.USER;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"social_type", "social_id"}
                )
        }
)
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 30)
    private SocialType socialType;

    @Column(name = "social_id", nullable = false)
    private String socialId;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "manner", nullable = false)
    @Builder.Default
    private Double manner = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = USER;

    // 소셜 로그인 유저 생성
    public static User createSocialUser(
            SocialUserInfo info,
            SocialType socialType
    ) {
        return User.builder()
                .socialType(socialType)
                .socialId(info.getSocialId())
                .role(USER)
                .build();
    }
}
