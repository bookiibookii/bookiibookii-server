package com.example.bookiibookii.domain.location.service;

import com.example.bookiibookii.domain.location.entity.Location;
import com.example.bookiibookii.domain.location.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private LocationService locationService;

    @Test
    void createsSeparateLocationWhenSameAddressHasDifferentCoordinates() {
        String address = "서울특별시 강남구 강남대로 396";
        BigDecimal x = new BigDecimal("127.027621");
        BigDecimal y = new BigDecimal("37.497942");
        when(locationRepository.findByAddressAndXAndY(address, x, y)).thenReturn(Optional.empty());
        when(locationRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(Location.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Location location = locationService.findOrCreate("강남역", address, null, x, y);

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(locationRepository).saveAndFlush(captor.capture());
        assertThat(location).isSameAs(captor.getValue());
        assertThat(captor.getValue().getAddress()).isEqualTo(address);
        assertThat(captor.getValue().getX()).isEqualByComparingTo(x);
        assertThat(captor.getValue().getY()).isEqualByComparingTo(y);
    }

    @Test
    void reusesLocationOnlyWhenAddressAndCoordinatesMatchAndFillsZipCodeOnly() {
        String address = "서울특별시 강남구 강남대로 396";
        BigDecimal x = new BigDecimal("127.027621");
        BigDecimal y = new BigDecimal("37.497942");
        Location existing = Location.builder()
                .placeName("강남역")
                .address(address)
                .x(x)
                .y(y)
                .build();
        when(locationRepository.findByAddressAndXAndY(address, x, y)).thenReturn(Optional.of(existing));

        Location location = locationService.findOrCreate("강남역", address, "06232", x, y);

        assertThat(location).isSameAs(existing);
        assertThat(location.getZipCode()).isEqualTo("06232");
        assertThat(location.getX()).isEqualByComparingTo(x);
        assertThat(location.getY()).isEqualByComparingTo(y);
    }
}
