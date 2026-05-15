package com.example.bookiibookii.domain.location.service;

import com.example.bookiibookii.domain.location.dto.req.UserDeliveryReqDTO;
import com.example.bookiibookii.domain.location.dto.res.UserDeliveryResDTO.UserDeliveryDto;
import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.location.entity.UserDelivery;
import com.example.bookiibookii.domain.location.exception.LocationException;
import com.example.bookiibookii.domain.location.exception.code.LocationErrorCode;
import com.example.bookiibookii.domain.location.repository.UserDeliveryRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDeliveryService {

    private static final int MAX_DELIVERY_COUNT = 2;

    private final UserDeliveryRepository userDeliveryRepository;
    private final LocationService locationService;
    private final UserRepository userRepository;

    public List<UserDeliveryDto> getMyDeliveries(Long userId) {
        return userDeliveryRepository.findByUserIdWithLocation(userId).stream()
                .map(UserDeliveryDto::from)
                .toList();
    }

    @Transactional
    public void addDelivery(Long userId, UserDeliveryReqDTO.AddReqDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        if (userDeliveryRepository.countByUser_Id(userId) >= MAX_DELIVERY_COUNT) {
            throw new LocationException(LocationErrorCode.LOCATION_LIMIT_EXCEEDED);
        }

        Location location = locationService.findOrCreate(req.placeName(), req.address(), req.zipCode());

        userDeliveryRepository.save(
                UserDelivery.builder()
                        .user(user)
                        .location(location)
                        .addressDetail(req.addressDetail())
                        .receiverName(req.receiverName())
                        .phone(req.phone())
                        .build()
        );
    }

    @Transactional
    public void deleteDelivery(Long userId, Long userDeliveryId) {
        UserDelivery userDelivery = userDeliveryRepository
                .findByIdAndUser_Id(userDeliveryId, userId)
                .orElseThrow(() -> new LocationException(LocationErrorCode.NOT_FOUND));

        userDeliveryRepository.delete(userDelivery);
    }
}
