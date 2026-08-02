package com.kodlamaio.maintenanceservice.api.clients;

import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarClientFallbackTest {

    // Now aligned with rental-service's CarClientFallback: throws BusinessException -> 422,
    // matching the documented precedent in CLAUDE.md ("match rental for any new fallback").
    @Test
    void checkIfCarAvailable_whenInvoked_throwsBusinessException() {
        var fallback = new CarClientFallback();
        var carId = UUID.randomUUID();

        assertThatThrownBy(() -> fallback.checkIfCarAvailable(carId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("INVENTORY-SERVICE NOT AVAILABLE RIGHT NOW!");
    }
}
