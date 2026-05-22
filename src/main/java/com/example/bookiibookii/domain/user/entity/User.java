package com.example.bookiibookii.domain.user.entity;

import com.example.bookiibookii.domain.user.enums.*;
import com.example.bookiibookii.global.auth.social.SocialUserInfo;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.example.bookiibookii.domain.user.enums.Gender.FEMALE;
import static com.example.bookiibookii.domain.user.enums.Role.USER;
import static com.example.bookiibookii.domain.user.enums.Status.ACTIVE;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Filter(name = "activeUserFilter", condition = "status = 'ACTIVE'")
@FilterDef(name = "activeUserFilter", defaultCondition = "status = 'ACTIVE'")
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

    @Column(name = "nickname")
    private String nickName;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", nullable = false, length = 30)
    private SocialType socialType;

    @Column(name = "social_id", nullable = false)
    private String socialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = USER;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserTag> userTags = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserImage userImage;

    @Column(name = "introduction")
    private String introduction;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(name = "birth")
    private LocalDate birth;


    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status")
    @Builder.Default
    private OnboardingStatus onboardingStatus = OnboardingStatus.NEW;

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

    public void withdraw() {
        this.status = Status.WITHDRAWN;
    }
    public void reactivate() {
        this.status = Status.ACTIVE;
    }
    public void updateName(String name) { this.nickName = name; }
    public void updateIntroduction(String introduction) { this.introduction = introduction; }
    public void updateUserInform(Gender gender, LocalDate birth) { this.gender = gender; this.birth = birth; }
    public void updateOnboardingStatus(OnboardingStatus status) { this.onboardingStatus = status; }
}