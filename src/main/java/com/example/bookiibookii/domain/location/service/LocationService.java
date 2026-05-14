package com.example.bookiibookii.domain.location.service;

import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.location.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Location findOrCreate(String placeName, String address, String zipCode) {
        return locationRepository.findByAddress(address)
                .orElseGet(() -> {
                    try {
                        return locationRepository.saveAndFlush(
                                Location.builder()
                                        .placeName(placeName)
                                        .address(address)
                                        .zipCode(zipCode)
                                        .build()
                        );
                    } catch (DataIntegrityViolationException e) {
                        return locationRepository.findByAddress(address).orElseThrow();
                    }
                });
    }
}
