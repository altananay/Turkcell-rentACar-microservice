package com.kodlamaio.inventoryservice.business.rules;

import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.inventoryservice.entities.Car;
import com.kodlamaio.inventoryservice.entities.enums.State;
import com.kodlamaio.inventoryservice.repository.CarRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarBusinessRulesTest {

    @Mock private CarRepository repository;

    @InjectMocks
    private CarBusinessRules rules;

    @Test
    void checkIfCarExists_whenCarExists_doesNotThrow() {
        var carId = UUID.randomUUID();
        when(repository.existsById(carId)).thenReturn(true);

        rules.checkIfCarExists(carId);
    }

    @Test
    void checkIfCarExists_whenCarMissing_throwsBusinessException() {
        var carId = UUID.randomUUID();
        when(repository.existsById(carId)).thenReturn(false);

        assertThatThrownBy(() -> rules.checkIfCarExists(carId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CAR_NOT_EXISTS");
    }

    @Test
    void checkCarAvailability_whenCarIsAvailable_doesNotThrow() {
        var carId = UUID.randomUUID();
        var car = new Car(carId, 2023, "34 ABC 123", State.Available, 100.0, null);
        when(repository.findById(carId)).thenReturn(Optional.of(car));

        rules.checkCarAvailability(carId);
    }

    @Test
    void checkCarAvailability_whenCarIsRented_throwsBusinessException() {
        var carId = UUID.randomUUID();
        var car = new Car(carId, 2023, "34 ABC 123", State.Rented, 100.0, null);
        when(repository.findById(carId)).thenReturn(Optional.of(car));

        assertThatThrownBy(() -> rules.checkCarAvailability(carId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CAR_NOT_AVAILABLE");
    }

    @Test
    void checkCarAvailability_whenCarMissing_throwsNoSuchElementException() {
        var carId = UUID.randomUUID();
        when(repository.findById(carId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rules.checkCarAvailability(carId))
                .isInstanceOf(NoSuchElementException.class);
    }
}
