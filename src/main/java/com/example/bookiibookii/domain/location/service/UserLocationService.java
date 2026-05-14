package com.example.bookiibookii.domain.location.service;

import com.example.bookiibookii.domain.location.dto.req.UserLocationReqDTO;
import com.example.bookiibookii.domain.location.dto.res.UserLocationResDTO.UserLocationDto;
import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.location.entity.UserLocation;
import com.example.bookiibookii.domain.location.enums.LocationType;
import com.example.bookiibookii.domain.location.exception.LocationException;
import com.example.bookiibookii.domain.location.exception.code.LocationErrorCode;
import com.example.bookiibookii.domain.location.repository.LocationRepository;
import com.example.bookiibookii.domain.location.repository.UserLocationRepository;
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
public class UserLocationService {

    private static final int MAX_LOCATION_COUNT = 2;

    private final UserLocationRepository userLocationRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;

    public List<UserLocationDto> getMyLocations(Long userId) {
        return userLocationRepository.findByUserIdWithLocation(userId).stream()
                .map(UserLocationDto::from)
                .toList();
    }

    @Transactional
    public void addLocation(Long userId, UserLocationReqDTO.AddReqDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        validateAddRequest(userId, req);

        Location location = locationRepository.findByAddress(req.address())
                .orElseGet(() -> locationRepository.save(
                        Location.builder()
                                .placeName(req.placeName())
                                .address(req.address())
                                .zipCode(req.zipCode())
                                .build()
                ));

        userLocationRepository.save(
                UserLocation.builder()
                        .user(user)
                        .location(location)
                        .type(req.type())
                        .addressDetail(req.addressDetail())
                        .receiverName(req.receiverName())
                        .phone(req.phone())
                        .build()
        );
    }

    @Transactional
    public void deleteLocation(Long userId, Long userLocationId) {
        UserLocation userLocation = userLocationRepository
                .findByUserLocationIdAndUser_Id(userLocationId, userId)
                .orElseThrow(() -> new LocationException(LocationErrorCode.NOT_FOUND));

        userLocationRepository.delete(userLocation);
    }

    private void validateAddRequest(Long userId, UserLocationReqDTO.AddReqDTO req) {
        if (userLocationRepository.countByUser_IdAndType(userId, req.type()) >= MAX_LOCATION_COUNT) {
            throw new LocationException(LocationErrorCode.LOCATION_LIMIT_EXCEEDED);
        }
        if (req.type() == LocationType.DELIVERY) {
            validateDeliveryFields(req.receiverName(), req.phone());
        }
    }

    private void validateDeliveryFields(String receiverName, String phone) {
        if (receiverName == null || receiverName.isBlank() || phone == null || phone.isBlank()) {
            throw new LocationException(LocationErrorCode.DELIVERY_FIELDS_REQUIRED);
        }
    }
}
