package com.example.bookiibookii.domain.location.service;

import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Location findOrCreate(String placeName, String address, String zipCode) {
        return findOrCreate(placeName, address, zipCode, null, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Location findOrCreate(String placeName, String address, String zipCode, BigDecimal x, BigDecimal y) {
        return locationRepository.findByAddressAndXAndY(address, x, y)
                .map(location -> {
                    location.fillMissingDetails(zipCode);
                    return location;
                })
                .orElseGet(() -> {
                    try {
                        return locationRepository.saveAndFlush(
                                Location.builder()
                                        .placeName(placeName)
                                        .address(address)
                                        .zipCode(zipCode)
                                        .x(x)
                                        .y(y)
                                        .build()
                        );
                    } catch (DataIntegrityViolationException e) {
                        Location location = locationRepository.findByAddressAndXAndY(address, x, y).orElseThrow();
                        location.fillMissingDetails(zipCode);
                        return location;
                    }
                });
    }
}
