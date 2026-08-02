package com.kodlamaio.rentalservice.api.clients;

import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CarClientFallbackTest {

    private final CarClientFallback fallback = new CarClientFallback();

    @Test
    void checkIfCarAvailable_alwaysThrowsBusinessException() {
        assertThatThrownBy(() -> fallback.checkIfCarAvailable(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("inventory service not available right now !");
    }

    @Test
    void getCar_alwaysThrowsBusinessException() {
        assertThatThrownBy(() -> fallback.getCar(UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("inventory service not available right now !");
    }
}
