package com.kodlamaio.rentalservice.business.rules;

import com.kodlamaio.commonpackage.utils.dto.ClientResponse;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.rentalservice.api.clients.CarClient;
import com.kodlamaio.rentalservice.repository.RentalRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalBusinessRulesTest {

    @Mock private RentalRepository repository;
    @Mock private CarClient carClient;

    @InjectMocks
    private RentalBusinessRules rules;

    @Test
    void checkIfRentalExists_whenRentalExists_doesNotThrow() {
        var rentalId = UUID.randomUUID();
        when(repository.existsById(rentalId)).thenReturn(true);

        rules.checkIfRentalExists(rentalId);
    }

    @Test
    void checkIfRentalExists_whenRentalMissing_throwsBusinessException() {
        var rentalId = UUID.randomUUID();
        when(repository.existsById(rentalId)).thenReturn(false);

        assertThatThrownBy(() -> rules.checkIfRentalExists(rentalId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("RENTAL_NOT_EXISTS");
    }

    @Test
    void ensureCarIsAvailable_whenCarClientReportsSuccess_doesNotThrow() {
        var carId = UUID.randomUUID();
        when(carClient.checkIfCarAvailable(carId)).thenReturn(new ClientResponse(true, null));

        rules.ensureCarIsAvailable(carId);
    }

    @Test
    void ensureCarIsAvailable_whenCarClientReportsFailure_throwsWithRemoteMessage() {
        var carId = UUID.randomUUID();
        when(carClient.checkIfCarAvailable(carId)).thenReturn(new ClientResponse(false, "some remote reason"));

        assertThatThrownBy(() -> rules.ensureCarIsAvailable(carId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("some remote reason");
    }
}
