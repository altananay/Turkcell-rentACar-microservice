package com.kodlamaio.rentalservice.api.clients;

import com.kodlamaio.commonpackage.utils.dto.CreateRentalPaymentRequest;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentClientFallbackTest {

    private final PaymentClientFallback fallback = new PaymentClientFallback();

    @Test
    void processRentalPayment_alwaysThrowsBusinessException() {
        assertThatThrownBy(() -> fallback.processRentalPayment("idempotency-key", new CreateRentalPaymentRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("PAYMENT DOWN");
    }

    @Test
    void refundRentalPayment_alwaysThrowsBusinessException() {
        assertThatThrownBy(() -> fallback.refundRentalPayment("idempotency-key", new CreateRentalPaymentRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("PAYMENT DOWN");
    }
}
