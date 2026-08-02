package com.kodlamaio.maintenanceservice.business.rules;

import com.kodlamaio.commonpackage.utils.dto.ClientResponse;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.maintenanceservice.api.clients.CarClient;
import com.kodlamaio.maintenanceservice.repository.MaintenanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceBusinessRulesTest {

    @Mock private MaintenanceRepository repository;
    @Mock private CarClient client;

    @InjectMocks
    private MaintenanceBusinessRules rules;

    @Test
    void ensureCarIsAvailable_whenClientReportsSuccess_doesNotThrow() {
        var carId = UUID.randomUUID();
        when(client.checkIfCarAvailable(carId)).thenReturn(new ClientResponse(true, null));

        assertThatCode(() -> rules.ensureCarIsAvailable(carId)).doesNotThrowAnyException();
    }

    @Test
    void ensureCarIsAvailable_whenClientReportsFailure_throwsBusinessExceptionWithRemoteMessage() {
        var carId = UUID.randomUUID();
        when(client.checkIfCarAvailable(carId)).thenReturn(new ClientResponse(false, "CAR_NOT_AVAILABLE"));

        assertThatThrownBy(() -> rules.ensureCarIsAvailable(carId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CAR_NOT_AVAILABLE");
    }

    @Test
    void checkIfMaintenanceExists_whenMaintenanceExists_doesNotThrow() {
        var id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        assertThatCode(() -> rules.checkIfMaintenanceExists(id)).doesNotThrowAnyException();
    }

    @Test
    void checkIfMaintenanceExists_whenMaintenanceMissing_throwsBusinessException() {
        var id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> rules.checkIfMaintenanceExists(id))
                .isInstanceOf(BusinessException.class)
                .hasMessage("MAINTENANCE_NOT_EXISTS");
    }

    // Inverted polarity versus every other rule in the codebase: this one throws when the
    // repository check returns TRUE (an incomplete maintenance already exists for the car).
    @Test
    void checkIfCarUnderMaintenance_whenCarHasIncompleteMaintenance_throwsBusinessException() {
        var carId = UUID.randomUUID();
        when(repository.existsByCarIdAndIsCompletedIsFalse(carId)).thenReturn(true);

        assertThatThrownBy(() -> rules.checkIfCarUnderMaintenance(carId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CAR_IS_CURRENTLY_UNDER_MAINTENANCE");
    }

    @Test
    void checkIfCarUnderMaintenance_whenCarHasNoIncompleteMaintenance_doesNotThrow() {
        var carId = UUID.randomUUID();
        when(repository.existsByCarIdAndIsCompletedIsFalse(carId)).thenReturn(false);

        assertThatCode(() -> rules.checkIfCarUnderMaintenance(carId)).doesNotThrowAnyException();
    }
}
