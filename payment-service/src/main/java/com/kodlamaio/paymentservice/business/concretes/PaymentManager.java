package com.kodlamaio.paymentservice.business.concretes;

import com.kodlamaio.commonpackage.utils.constants.Messages;
import com.kodlamaio.commonpackage.utils.dto.ClientResponse;
import com.kodlamaio.commonpackage.utils.dto.CreateRentalPaymentRequest;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import com.kodlamaio.paymentservice.business.abstracts.PaymentService;
import com.kodlamaio.paymentservice.business.abstracts.PosService;
import com.kodlamaio.paymentservice.business.dto.requests.CreatePaymentRequest;
import com.kodlamaio.paymentservice.business.dto.requests.UpdatePaymentRequest;
import com.kodlamaio.paymentservice.business.dto.responses.CreatePaymentResponse;
import com.kodlamaio.paymentservice.business.dto.responses.GetAllPaymentsResponse;
import com.kodlamaio.paymentservice.business.dto.responses.GetPaymentResponse;
import com.kodlamaio.paymentservice.business.dto.responses.UpdatePaymentResponse;
import com.kodlamaio.paymentservice.business.rules.PaymentBusinessRules;
import com.kodlamaio.paymentservice.entity.OperationType;
import com.kodlamaio.paymentservice.entity.Payment;
import com.kodlamaio.paymentservice.entity.ProcessedPaymentOperation;
import com.kodlamaio.paymentservice.repository.PaymentRepository;
import com.kodlamaio.paymentservice.repository.ProcessedPaymentOperationRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class PaymentManager implements PaymentService {
    private final PaymentRepository repository;
    private final ModelMapperService mapper;
    private final PosService posService;
    private final PaymentBusinessRules rules;
    private final ProcessedPaymentOperationRepository processedOperationRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public List<GetAllPaymentsResponse> getAll() {
        List<Payment> payments = repository.findAll();
        List<GetAllPaymentsResponse> response = payments
                .stream()
                .map(payment -> mapper.forResponse().map(payment, GetAllPaymentsResponse.class))
                .toList();

        return response;
    }

    @Override
    public GetPaymentResponse getById(UUID id) {
        rules.checkIfPaymentExists(id);
        Payment payment = repository.findById(id).orElseThrow();
        GetPaymentResponse response = mapper.forResponse().map(payment, GetPaymentResponse.class);

        return response;
    }

    @Override
    public CreatePaymentResponse add(CreatePaymentRequest request) {
        rules.checkIfCardExists(request.getCardNumber());
        Payment payment = mapper.forResponse().map(request, Payment.class);
        payment.setId(UUID.randomUUID());
        repository.save(payment);
        CreatePaymentResponse response = mapper.forResponse().map(payment, CreatePaymentResponse.class);

        return response;
    }

    @Override
    public UpdatePaymentResponse update(UUID id, UpdatePaymentRequest request) {
        rules.checkIfPaymentExists(id);
        Payment payment = mapper.forResponse().map(request, Payment.class);
        payment.setId(id);
        repository.save(payment);
        UpdatePaymentResponse response = mapper.forResponse().map(payment, UpdatePaymentResponse.class);
        return response;
    }

    @Override
    public void delete(UUID id) {
        rules.checkIfPaymentExists(id);
        repository.deleteById(id);
    }

    @Override
    public ClientResponse processRentalPayment(String idempotencyKey, CreateRentalPaymentRequest request) {
        var existing = processedOperationRepository.findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.CHARGE);
        if (existing.isPresent()) {
            return toClientResponse(existing.get());
        }

        ClientResponse response = new ClientResponse();
        validatePayment(idempotencyKey, request, response);
        return response;
    }

    @Override
    public ClientResponse refundRentalPayment(String idempotencyKey, CreateRentalPaymentRequest request) {
        var existing = processedOperationRepository.findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.REFUND);
        if (existing.isPresent()) {
            return toClientResponse(existing.get());
        }

        ClientResponse response = new ClientResponse();
        processRefund(idempotencyKey, request, response);
        return response;
    }

    private void validatePayment(String idempotencyKey, CreateRentalPaymentRequest request, ClientResponse response) {
        try {
            rules.checkIfPaymentIsValid(request);
            Payment payment = Optional.ofNullable(repository.findByCardNumber(request.getCardNumber()))
                    .orElseThrow(() -> new BusinessException(Messages.Payment.NotFound));

            rules.checkIfBalanceIsEnough(payment.getBalance(), request.getPrice());
            //FAKE POS SERVICE
            posService.pay();

            var recorded = chargeAndRecord(idempotencyKey, payment, request.getPrice());
            if (recorded.isEmpty()) {
                var winner = processedOperationRepository
                        .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.CHARGE)
                        .orElseThrow(() -> new BusinessException(Messages.Payment.NotFound));
                response.setSuccess(winner.isSuccess());
                response.setMessage(winner.getMessage());
                return;
            }
            response.setSuccess(true);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
        }
    }

    private Optional<ProcessedPaymentOperation> chargeAndRecord(String idempotencyKey, Payment payment, double price) {
        try {
            return Optional.of(transactionTemplate.execute(status -> {
                payment.setBalance(payment.getBalance() - price);
                repository.save(payment);
                var operation = new ProcessedPaymentOperation();
                operation.setIdempotencyKey(idempotencyKey);
                operation.setOperationType(OperationType.CHARGE);
                operation.setSuccess(true);
                return processedOperationRepository.save(operation);
            }));
        } catch (DataIntegrityViolationException raceLoss) {
            return Optional.empty();
        }
    }

    private void processRefund(String idempotencyKey, CreateRentalPaymentRequest request, ClientResponse response) {
        try {
            Payment payment = Optional.ofNullable(repository.findByCardNumber(request.getCardNumber()))
                    .orElseThrow(() -> new BusinessException(Messages.Payment.NotFound));

            var recorded = refundAndRecord(idempotencyKey, payment, request.getPrice());
            if (recorded.isEmpty()) {
                var winner = processedOperationRepository
                        .findByIdempotencyKeyAndOperationType(idempotencyKey, OperationType.REFUND)
                        .orElseThrow(() -> new BusinessException(Messages.Payment.NotFound));
                response.setSuccess(winner.isSuccess());
                response.setMessage(winner.getMessage());
                return;
            }
            response.setSuccess(true);
        } catch (Exception e) {
            response.setSuccess(false);
            response.setMessage(e.getMessage());
        }
    }

    private Optional<ProcessedPaymentOperation> refundAndRecord(String idempotencyKey, Payment payment, double price) {
        try {
            return Optional.of(transactionTemplate.execute(status -> {
                payment.setBalance(payment.getBalance() + price);
                repository.save(payment);
                var operation = new ProcessedPaymentOperation();
                operation.setIdempotencyKey(idempotencyKey);
                operation.setOperationType(OperationType.REFUND);
                operation.setSuccess(true);
                return processedOperationRepository.save(operation);
            }));
        } catch (DataIntegrityViolationException raceLoss) {
            return Optional.empty();
        }
    }

    private ClientResponse toClientResponse(ProcessedPaymentOperation operation) {
        var response = new ClientResponse();
        response.setSuccess(operation.isSuccess());
        response.setMessage(operation.getMessage());
        return response;
    }
}
