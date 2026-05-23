package com.example.bookiibookii.domain.location.service;

import com.example.bookiibookii.domain.location.dto.req.UserExchangeReqDTO;
import com.example.bookiibookii.domain.location.dto.res.UserExchangeResDTO.UserExchangeDto;
import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.location.entity.UserExchange;
import com.example.bookiibookii.domain.location.exception.LocationException;
import com.example.bookiibookii.domain.location.exception.code.LocationErrorCode;
import com.example.bookiibookii.domain.location.repository.UserExchangeRepository;
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
public class UserExchangeService {

    private static final int MAX_EXCHANGE_COUNT = 2;

    private final UserExchangeRepository userExchangeRepository;
    private final LocationService locationService;
    private final UserRepository userRepository;

    public List<UserExchangeDto> getMyExchanges(Long userId) {
        return userExchangeRepository.findByUserIdWithLocation(userId).stream()
                .map(UserExchangeDto::from)
                .toList();
    }

    @Transactional
    public void addExchange(Long userId, UserExchangeReqDTO.AddReqDTO req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        long count = userExchangeRepository.countByUser_Id(userId);
        if (count >= MAX_EXCHANGE_COUNT) {
            throw new LocationException(LocationErrorCode.LOCATION_LIMIT_EXCEEDED);
        }

        Location location = locationService.findOrCreate(req.placeName(), req.address(), req.zipCode());

        userExchangeRepository.save(
                UserExchange.builder()
                        .user(user)
                        .location(location)
                        .addressDetail(req.addressDetail())
                        .isDefault(count == 0)
                        .build()
        );
    }

    @Transactional
    public void updateExchange(Long userId, Long userExchangeId, UserExchangeReqDTO.AddReqDTO req) {
        UserExchange userExchange = userExchangeRepository
                .findByIdAndUser_Id(userExchangeId, userId)
                .orElseThrow(() -> new LocationException(LocationErrorCode.NOT_FOUND));

        Location location = locationService.findOrCreate(req.placeName(), req.address(), req.zipCode());
        userExchange.update(location, req.addressDetail());
    }

    @Transactional
    public void deleteExchange(Long userId, Long userExchangeId) {
        UserExchange userExchange = userExchangeRepository
                .findByIdAndUser_Id(userExchangeId, userId)
                .orElseThrow(() -> new LocationException(LocationErrorCode.NOT_FOUND));

        if (userExchange.isDefault()) {
            userExchangeRepository.findFirstByUser_IdAndIdNotOrderByCreatedAtAsc(userId, userExchangeId)
                    .ifPresent(other -> other.setDefault(true));
        }

        userExchangeRepository.delete(userExchange);
    }

    @Transactional
    public void setDefaultExchange(Long userId, Long userExchangeId) {
        if (!userExchangeRepository.existsByIdAndUser_Id(userExchangeId, userId)) {
            throw new LocationException(LocationErrorCode.NOT_FOUND);
        }
        userExchangeRepository.updateDefaultExchange(userId, userExchangeId);
    }
}
