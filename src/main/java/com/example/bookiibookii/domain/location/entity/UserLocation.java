package com.example.bookiibookii.domain.location.entity;

import com.example.bookiibookii.domain.location.enums.LocationType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserLocation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_location_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private LocationType type;

    @Column(name = "address_detail", length = 200)
    private String addressDetail;

    @Column(name = "receiver_name", length = 50)
    private String receiverName;

    @Column(name = "phone", length = 20)
    private String phone;

    public void update(String addressDetail, String receiverName, String phone) {
        this.addressDetail = addressDetail;
        this.receiverName = receiverName;
        this.phone = phone;
    }
}
