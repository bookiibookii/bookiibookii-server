package com.example.bookiibookii.domain.location.entity;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_delivery")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserDelivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_delivery_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "address_detail", length = 200)
    private String addressDetail;

    @Column(name = "receiver_name", nullable = false, length = 50)
    private String receiverName;

    @Column(name = "phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    public void update(Location location, String addressDetail, String receiverName, String phone) {
        this.location = location;
        this.addressDetail = addressDetail;
        this.receiverName = receiverName;
        this.phone = phone;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
