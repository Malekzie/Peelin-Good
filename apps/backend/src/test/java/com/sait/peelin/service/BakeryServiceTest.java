package com.sait.peelin.service;

import com.sait.peelin.dto.v1.BakeryDto;
import com.sait.peelin.model.Address;
import com.sait.peelin.model.Bakery;
import com.sait.peelin.repository.AddressRepository;
import com.sait.peelin.repository.BakeryHourRepository;
import com.sait.peelin.repository.BakeryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BakeryServiceTest {

    @Mock private BakeryRepository bakeryRepository;
    @Mock private BakeryHourRepository bakeryHourRepository;
    @Mock private AddressRepository addressRepository;

    @InjectMocks
    private BakeryService bakeryService;

    @Test
    void list_ShouldReturnBakeries() {
        // Arrange
        Bakery b1 = new Bakery();
        b1.setId(1);
        b1.setBakeryName("Bakery 1");
        b1.setAddress(new Address());

        when(bakeryRepository.findAll()).thenReturn(List.of(b1));

        // Act
        List<BakeryDto> result = bakeryService.list(null);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Bakery 1", result.get(0).name());
        verify(bakeryRepository).findAll();
    }

    @Test
    void get_ShouldReturnBakery() {
        // Arrange
        Bakery b1 = new Bakery();
        b1.setId(1);
        b1.setBakeryName("Bakery 1");
        b1.setAddress(new Address());

        when(bakeryRepository.findById(1)).thenReturn(Optional.of(b1));

        // Act
        BakeryDto result = bakeryService.get(1);

        // Assert
        assertNotNull(result);
        assertEquals("Bakery 1", result.name());
    }
}
