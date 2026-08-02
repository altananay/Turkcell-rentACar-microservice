package com.kodlamaio.rentalservice.business.saga;

import com.kodlamaio.commonpackage.events.rental.RentalCreatedEvent;
import com.kodlamaio.commonpackage.events.rentalPayment.RentalPaymentCreatedEvent;
import com.kodlamaio.commonpackage.kafka.producer.KafkaProducer;
import com.kodlamaio.commonpackage.utils.dto.CarClientResponse;
import com.kodlamaio.commonpackage.utils.dto.ClientResponse;
import com.kodlamaio.commonpackage.utils.dto.CreateRentalPaymentRequest;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.rentalservice.api.clients.CarClient;
import com.kodlamaio.rentalservice.api.clients.PaymentClient;
import com.kodlamaio.rentalservice.business.dto.requests.CreateRentalRequest;
import com.kodlamaio.rentalservice.entities.Rental;
import com.kodlamaio.rentalservice.entities.RentalCreationSaga;
import com.kodlamaio.rentalservice.entities.enums.SagaStatus;
import com.kodlamaio.rentalservice.repository.RentalCreationSagaRepository;
import com.kodlamaio.rentalservice.repository.RentalRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class RentalCreationSagaOrchestrator {
    private final RentalCreationSagaRepository sagaRepository;
    private final RentalRepository rentalRepository;
    private final PaymentClient paymentClient;
    private final CarClient carClient;
    private final KafkaProducer producer;
    private final TransactionTemplate transactionTemplate;

    public Rental createRental(CreateRentalRequest request) {
        var saga = buildInitialSaga(request);
        sagaRepository.save(saga);
        return drive(saga);
    }

    public Rental drive(RentalCreationSaga saga) {
        try {
            if (saga.getStatus() == SagaStatus.COMPENSATING) {
                retryCompensation(saga);
                return null;
            }
            if (saga.getStatus() == SagaStatus.STARTED) {
                chargePayment(saga);
                saga.setStatus(SagaStatus.PAYMENT_CHARGED);
                sagaRepository.save(saga);
            }
            return completeRentalCreation(saga);
        } catch (OptimisticLockingFailureException raced) {
            log.warn("Saga {} concurrently handled elsewhere; skipping", saga.getId());
            return null;
        } catch (Exception e) {
            try {
                compensate(saga, e);
            } catch (OptimisticLockingFailureException raced) {
                log.warn("Saga {} concurrently compensated elsewhere", saga.getId());
            }
            throw e;
        }
    }

    private void chargePayment(RentalCreationSaga saga) {
        ClientResponse response = paymentClient.processRentalPayment(saga.getId().toString(), toPaymentRequest(saga));
        if (!response.isSuccess()) {
            throw new BusinessException(response.getMessage());
        }
    }

    private Rental completeRentalCreation(RentalCreationSaga saga) {
        var existing = rentalRepository.findById(saga.getRentalId());
        if (existing.isPresent()) {
            if (saga.getStatus() != SagaStatus.COMPLETED) {
                saga.setStatus(SagaStatus.COMPLETED);
                sagaRepository.save(saga);
            }
            return existing.get();
        }

        CarClientResponse carClientResponse = carClient.getCar(saga.getCarId());
        Rental rental = buildRental(saga);

        transactionTemplate.executeWithoutResult(status -> {
            rentalRepository.save(rental);
            saga.setStatus(SagaStatus.COMPLETED);
            sagaRepository.save(saga);
        });
        // Committed. Nothing from here on may trigger compensation.

        publishBestEffort(saga, rental, carClientResponse);
        return rental;
    }

    private void publishBestEffort(RentalCreationSaga saga, Rental rental, CarClientResponse carClientResponse) {
        try {
            producer.sendMessage(new RentalCreatedEvent(saga.getCarId()), "rental-created");
            producer.sendMessage(buildPaymentCreatedEvent(saga, rental, carClientResponse), "rental-payment-created");
        } catch (Exception e) {
            log.error("Best-effort Kafka publish failed after saga {} completed: {}", saga.getId(), e.getMessage());
        }
    }

    private void compensate(RentalCreationSaga saga, Exception cause) {
        if (saga.getStatus() == SagaStatus.STARTED) {
            saga.setStatus(SagaStatus.PAYMENT_FAILED);
            saga.setFailureReason(cause.getMessage());
            sagaRepository.save(saga);
            return;
        }
        saga.setStatus(SagaStatus.COMPENSATING);
        saga.setFailureReason(cause.getMessage());
        sagaRepository.save(saga);
        retryCompensation(saga);
    }

    private void retryCompensation(RentalCreationSaga saga) {
        ClientResponse response;
        try {
            response = paymentClient.refundRentalPayment(saga.getId().toString(), toPaymentRequest(saga));
        } catch (Exception refundException) {
            log.error("Refund failed for saga {}: {}", saga.getId(), refundException.getMessage());
            saga.setStatus(SagaStatus.COMPENSATION_FAILED);
            saga.setFailureReason(refundException.getMessage());
            sagaRepository.save(saga);
            return;
        }
        saga.setStatus(response.isSuccess() ? SagaStatus.COMPENSATED : SagaStatus.COMPENSATION_FAILED);
        if (!response.isSuccess()) {
            log.error("Refund reported failure for saga {}: {}", saga.getId(), response.getMessage());
            saga.setFailureReason(response.getMessage());
        }
        sagaRepository.save(saga);
    }

    private RentalCreationSaga buildInitialSaga(CreateRentalRequest request) {
        var paymentRequest = request.getPaymentRequest();
        var saga = new RentalCreationSaga();
        saga.setId(UUID.randomUUID());
        saga.setRentalId(UUID.randomUUID());
        saga.setCarId(request.getCarId());
        saga.setDailyPrice(request.getDailyPrice());
        saga.setRentedForDays(request.getRentedForDays());
        saga.setPrice(request.getDailyPrice() * request.getRentedForDays());
        saga.setCardNumber(paymentRequest.getCardNumber());
        saga.setCardHolder(paymentRequest.getCardHolder());
        saga.setCardExpirationYear(paymentRequest.getCardExpirationYear());
        saga.setCardExpirationMonth(paymentRequest.getCardExpirationMonth());
        saga.setCardCvv(paymentRequest.getCardCvv());
        saga.setStatus(SagaStatus.STARTED);
        return saga;
    }

    private Rental buildRental(RentalCreationSaga saga) {
        var rental = new Rental();
        rental.setId(saga.getRentalId());
        rental.setCarId(saga.getCarId());
        rental.setDailyPrice(saga.getDailyPrice());
        rental.setRentedForDays(saga.getRentedForDays());
        rental.setTotalPrice(saga.getPrice());
        rental.setRentedAt(saga.getCreatedAt().toLocalDate());
        return rental;
    }

    private CreateRentalPaymentRequest toPaymentRequest(RentalCreationSaga saga) {
        var request = new CreateRentalPaymentRequest();
        request.setCardNumber(saga.getCardNumber());
        request.setCardHolder(saga.getCardHolder());
        request.setCardExpirationYear(saga.getCardExpirationYear());
        request.setCardExpirationMonth(saga.getCardExpirationMonth());
        request.setCardCvv(saga.getCardCvv());
        request.setPrice(saga.getPrice());
        return request;
    }

    private RentalPaymentCreatedEvent buildPaymentCreatedEvent(RentalCreationSaga saga, Rental rental, CarClientResponse carClientResponse) {
        var event = new RentalPaymentCreatedEvent();
        event.setCardHolder(saga.getCardHolder());
        event.setModelName(carClientResponse.getModelName());
        event.setBrandName(carClientResponse.getBrandName());
        event.setPlate(carClientResponse.getPlate());
        event.setModelYear(carClientResponse.getModelYear());
        event.setDailyPrice(rental.getDailyPrice());
        event.setTotalPrice(rental.getTotalPrice());
        event.setRentedForDays(rental.getRentedForDays());
        event.setRentedAt(saga.getCreatedAt());
        return event;
    }
}
