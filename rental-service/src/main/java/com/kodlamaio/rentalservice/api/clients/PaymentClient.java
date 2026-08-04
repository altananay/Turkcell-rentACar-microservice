package com.kodlamaio.rentalservice.api.clients;

import com.kodlamaio.commonpackage.utils.dto.ClientResponse;
import com.kodlamaio.commonpackage.utils.dto.CreateRentalPaymentRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "payment-service",
        configuration = PaymentClientTokenConfiguration.class,
        fallback = PaymentClientFallback.class)
public interface PaymentClient {
    @PostMapping(value = "/api/payments/process-rental-payment")
    ClientResponse processRentalPayment(@RequestHeader("Idempotency-Key") String idempotencyKey, CreateRentalPaymentRequest request);

    @PostMapping(value = "/api/payments/refund-rental-payment")
    ClientResponse refundRentalPayment(@RequestHeader("Idempotency-Key") String idempotencyKey, CreateRentalPaymentRequest request);
}