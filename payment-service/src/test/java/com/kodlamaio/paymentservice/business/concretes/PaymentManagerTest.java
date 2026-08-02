package com.kodlamaio.paymentservice.business.concretes;

import com.kodlamaio.commonpackage.utils.dto.CreateRentalPaymentRequest;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentManagerTest {

    private static final String IDEMPOTENCY_KEY = "saga-123";

    @Mock private PaymentRepository repository;
    @Mock private ModelMapperService mapper;
    @Mock private PosService posService;
    @Mock private PaymentBusinessRules rules;
    @Mock private ModelMapper modelMapper;
    @Mock private ProcessedPaymentOperationRepository processedOperationRepository;
    @Mock private TransactionTemplate transactionTemplate;

    @InjectMocks
    private PaymentManager paymentManager;

    @BeforeEach
    void stubTransactionTemplateToRunCallback() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        lenient().when(processedOperationRepository.findByIdempotencyKeyAndOperationType(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(processedOperationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void getAll_mapsEveryPaymentToResponse() {
        var payment1 = new Payment();
        var payment2 = new Payment();
        var resp1 = new GetAllPaymentsResponse();
        var resp2 = new GetAllPaymentsResponse();

        when(repository.findAll()).thenReturn(List.of(payment1, payment2));
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(payment1, GetAllPaymentsResponse.class)).thenReturn(resp1);
        when(modelMapper.map(payment2, GetAllPaymentsResponse.class)).thenReturn(resp2);

        var result = paymentManager.getAll();

        assertThat(result).containsExactly(resp1, resp2);
    }

    @Test
    void getById_whenPaymentExists_returnsMappedResponse() {
        var paymentId = UUID.randomUUID();
        var payment = new Payment();
        var expected = new GetPaymentResponse();

        when(repository.findById(paymentId)).thenReturn(Optional.of(payment));
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(payment, GetPaymentResponse.class)).thenReturn(expected);

        var response = paymentManager.getById(paymentId);

        assertThat(response).isSameAs(expected);
        verify(rules).checkIfPaymentExists(paymentId);
    }

    @Test
    void getById_whenRulesReject_propagatesBusinessException() {
        var paymentId = UUID.randomUUID();
        doThrow(new BusinessException("PAYMENT_NOT_FOUND")).when(rules).checkIfPaymentExists(paymentId);

        assertThrows(BusinessException.class, () -> paymentManager.getById(paymentId));
        verifyNoInteractions(repository);
    }

    @Test
    void add_assignsIdAndSaves_returnsMappedResponse() {
        var request = new CreatePaymentRequest();
        request.setCardNumber("1234123412341234");
        var mappedPayment = new Payment();
        var response = new CreatePaymentResponse();

        // add() maps the inbound request via forResponse() — forRequest() is never called in this manager.
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(request, Payment.class)).thenReturn(mappedPayment);
        when(modelMapper.map(mappedPayment, CreatePaymentResponse.class)).thenReturn(response);

        var result = paymentManager.add(request);

        verify(rules).checkIfCardExists(request.getCardNumber());

        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNotNull();

        assertThat(result).isSameAs(response);
    }

    @Test
    void update_mapsAndSaves_returnsMappedResponse() {
        var id = UUID.randomUUID();
        var request = new UpdatePaymentRequest();
        var mappedPayment = new Payment();
        var response = new UpdatePaymentResponse();

        // update() also maps the inbound request via forResponse(), not forRequest().
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(request, Payment.class)).thenReturn(mappedPayment);
        when(modelMapper.map(mappedPayment, UpdatePaymentResponse.class)).thenReturn(response);

        var result = paymentManager.update(id, request);

        verify(rules).checkIfPaymentExists(id);

        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(id);

        assertThat(result).isSameAs(response);
    }

    @Test
    void delete_whenPaymentExists_deletesById() {
        var id = UUID.randomUUID();

        paymentManager.delete(id);

        verify(rules).checkIfPaymentExists(id);
        verify(repository).deleteById(id);
    }

    @Test
    void delete_whenRulesReject_propagatesBusinessExceptionAndSkipsDelete() {
        var id = UUID.randomUUID();
        doThrow(new BusinessException("PAYMENT_NOT_FOUND")).when(rules).checkIfPaymentExists(id);

        assertThrows(BusinessException.class, () -> paymentManager.delete(id));
        verify(repository, never()).deleteById(any());
    }

    @Test
    void processRentalPayment_whenEverythingSucceeds_returnsSuccessAndDebitsBalance() {
        var request = new CreateRentalPaymentRequest();
        request.setCardNumber("1234123412341234");
        request.setPrice(200);

        var payment = new Payment();
        payment.setBalance(500);

        doNothing().when(rules).checkIfPaymentIsValid(request);
        when(repository.findByCardNumber(request.getCardNumber())).thenReturn(payment);
        doNothing().when(rules).checkIfBalanceIsEnough(500, 200);
        doNothing().when(posService).pay();

        var response = paymentManager.processRentalPayment(IDEMPOTENCY_KEY, request);

        assertThat(response.isSuccess()).isTrue();

        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBalance()).isEqualTo(300.0);
    }

    @Test
    void processRentalPayment_whenCardNotFound_returnsFailureWithNotFoundMessage() {
        // PaymentManager now null-checks repository.findByCardNumber(...) and throws a
        // BusinessException(Messages.Payment.NotFound) instead of letting payment.getBalance()
        // NPE. The broad catch(Exception) still turns it into a clean failure response.
        var request = new CreateRentalPaymentRequest();
        request.setCardNumber("0000000000000000");
        request.setPrice(200);

        // repository.findByCardNumber(...) intentionally left unstubbed -> returns null by default

        var response = paymentManager.processRentalPayment(IDEMPOTENCY_KEY, request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("PAYMENT_NOT_FOUND");
    }

    @Test
    void processRentalPayment_whenRuleThrowsBusinessException_returnsFailureWithRuleMessage() {
        var request = new CreateRentalPaymentRequest();
        request.setCardNumber("1234123412341234");
        request.setPrice(200);

        doThrow(new BusinessException("NOT_A_VALID_PAYMENT")).when(rules).checkIfPaymentIsValid(request);

        var response = paymentManager.processRentalPayment(IDEMPOTENCY_KEY, request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("NOT_A_VALID_PAYMENT");
    }

    @Test
    void processRentalPayment_whenNoPriorOperationRecorded_chargesAndPersistsIdempotencyRecord() {
        var request = new CreateRentalPaymentRequest();
        request.setCardNumber("1234123412341234");
        request.setPrice(200);

        var payment = new Payment();
        payment.setBalance(500);

        when(repository.findByCardNumber(request.getCardNumber())).thenReturn(payment);

        var response = paymentManager.processRentalPayment(IDEMPOTENCY_KEY, request);

        assertThat(response.isSuccess()).isTrue();

        var opCaptor = ArgumentCaptor.forClass(ProcessedPaymentOperation.class);
        verify(processedOperationRepository).save(opCaptor.capture());
        assertThat(opCaptor.getValue().getIdempotencyKey()).isEqualTo(IDEMPOTENCY_KEY);
        assertThat(opCaptor.getValue().getOperationType()).isEqualTo(OperationType.CHARGE);
        assertThat(opCaptor.getValue().isSuccess()).isTrue();
    }

    @Test
    void processRentalPayment_whenIdempotencyKeyAlreadyProcessed_returnsStoredResultWithoutTouchingBalance() {
        var request = new CreateRentalPaymentRequest();
        request.setCardNumber("1234123412341234");
        request.setPrice(200);

        var previousOperation = new ProcessedPaymentOperation();
        previousOperation.setIdempotencyKey(IDEMPOTENCY_KEY);
        previousOperation.setOperationType(OperationType.CHARGE);
        previousOperation.setSuccess(true);
        when(processedOperationRepository.findByIdempotencyKeyAndOperationType(IDEMPOTENCY_KEY, OperationType.CHARGE))
                .thenReturn(Optional.of(previousOperation));

        var response = paymentManager.processRentalPayment(IDEMPOTENCY_KEY, request);

        assertThat(response.isSuccess()).isTrue();
        verifyNoInteractions(repository, posService, rules);
        verify(processedOperationRepository, never()).save(any());
    }

    @Test
    void processRentalPayment_whenConcurrentInsertRaces_returnsWinningResultInsteadOfPropagatingConstraintViolation() {
        var request = new CreateRentalPaymentRequest();
        request.setCardNumber("1234123412341234");
        request.setPrice(200);

        var payment = new Payment();
        payment.setBalance(500);
        when(repository.findByCardNumber(request.getCardNumber())).thenReturn(payment);
        // doThrow().when(mock) (not when(mock).thenThrow()) — re-stubbing execute(any()) with the
        // when()-first form would re-invoke the @BeforeEach's already-registered Answer as a side
        // effect of setting up this new stub, NPE-ing on a null callback argument.
        doThrow(new DataIntegrityViolationException("unique constraint")).when(transactionTemplate).execute(any());

        var winningOperation = new ProcessedPaymentOperation();
        winningOperation.setIdempotencyKey(IDEMPOTENCY_KEY);
        winningOperation.setOperationType(OperationType.CHARGE);
        winningOperation.setSuccess(true);
        when(processedOperationRepository.findByIdempotencyKeyAndOperationType(IDEMPOTENCY_KEY, OperationType.CHARGE))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winningOperation));

        var response = paymentManager.processRentalPayment(IDEMPOTENCY_KEY, request);

        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void refundRentalPayment_whenCardExists_returnsSuccessAndCreditsBalance() {
        var request = new CreateRentalPaymentRequest();
        request.setCardNumber("1234123412341234");
        request.setPrice(200);

        var payment = new Payment();
        payment.setBalance(300);

        when(repository.findByCardNumber(request.getCardNumber())).thenReturn(payment);

        var response = paymentManager.refundRentalPayment(IDEMPOTENCY_KEY, request);

        assertThat(response.isSuccess()).isTrue();
        var captor = ArgumentCaptor.forClass(Payment.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getBalance()).isEqualTo(500.0);
    }

    @Test
    void refundRentalPayment_whenCardNotFound_returnsFailureWithNotFoundMessage() {
        var request = new CreateRentalPaymentRequest();
        request.setCardNumber("0000000000000000");
        request.setPrice(200);

        // repository.findByCardNumber(...) intentionally left unstubbed -> returns null by default

        var response = paymentManager.refundRentalPayment(IDEMPOTENCY_KEY, request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("PAYMENT_NOT_FOUND");
    }

    @Test
    void refundRentalPayment_whenIdempotencyKeyAlreadyProcessed_returnsStoredResultWithoutTouchingBalance() {
        var request = new CreateRentalPaymentRequest();
        request.setCardNumber("1234123412341234");
        request.setPrice(200);

        var previousOperation = new ProcessedPaymentOperation();
        previousOperation.setIdempotencyKey(IDEMPOTENCY_KEY);
        previousOperation.setOperationType(OperationType.REFUND);
        previousOperation.setSuccess(true);
        when(processedOperationRepository.findByIdempotencyKeyAndOperationType(IDEMPOTENCY_KEY, OperationType.REFUND))
                .thenReturn(Optional.of(previousOperation));

        var response = paymentManager.refundRentalPayment(IDEMPOTENCY_KEY, request);

        assertThat(response.isSuccess()).isTrue();
        verifyNoInteractions(repository);
        verify(processedOperationRepository, never()).save(any());
    }
}
