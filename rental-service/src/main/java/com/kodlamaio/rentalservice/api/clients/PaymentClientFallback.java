package com.kodlamaio.rentalservice.api.clients;

import com.kodlamaio.commonpackage.utils.dto.ClientResponse;
import com.kodlamaio.commonpackage.utils.dto.CreateRentalPaymentRequest;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentClientFallback implements PaymentClient {
    @Override
    public ClientResponse processRentalPayment(String idempotencyKey, CreateRentalPaymentRequest request) {
        log.info("PAYMENT SERVICE IS DOWN");
        throw new BusinessException("PAYMENT DOWN");
    }

    @Override
    public ClientResponse refundRentalPayment(String idempotencyKey, CreateRentalPaymentRequest request) {
        log.info("PAYMENT SERVICE IS DOWN");
        throw new BusinessException("PAYMENT DOWN");
    }
}