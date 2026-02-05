package com.example.bookiibookii.domain.user.entity;

import com.example.bookiibookii.domain.user.enums.Role;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.enums.Status;
import com.example.bookiibookii.global.auth.social.SocialUserInfo;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "manner", nullable = false)
    @Builder.Default
    private Double manner = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = USER;

    @Column(name = "meet_place")
    private String meetPlace;

    @Column(name = "Region")
    private String region;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserTag> userTags = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserImage userImage;


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
    public void updateMeetPlace(String meetPlace) { this.meetPlace = meetPlace; }
}
