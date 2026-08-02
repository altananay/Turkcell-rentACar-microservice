package com.kodlamaio.paymentservice.business.rules;

import com.kodlamaio.commonpackage.utils.dto.PaymentRequest;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentBusinessRulesTest {

    @Mock private PaymentRepository repository;

    @InjectMocks
    private PaymentBusinessRules rules;

    @Test
    void checkIfPaymentExists_whenPaymentExists_doesNotThrow() {
        var id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        rules.checkIfPaymentExists(id);
    }

    @Test
    void checkIfPaymentExists_whenPaymentMissing_throwsBusinessException() {
        var id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> rules.checkIfPaymentExists(id))
                .isInstanceOf(BusinessException.class)
                .hasMessage("PAYMENT_NOT_FOUND");
    }

    // checkIfBalanceIsEnough is a pure function — no repository interaction, no stubbing needed.

    @Test
    void checkIfBalanceIsEnough_whenBalanceGreaterThanPrice_doesNotThrow() {
        rules.checkIfBalanceIsEnough(500, 200);
    }

    @Test
    void checkIfBalanceIsEnough_whenBalanceEqualsPrice_doesNotThrow() {
        rules.checkIfBalanceIsEnough(200, 200);
    }

    @Test
    void checkIfBalanceIsEnough_whenBalanceLessThanPrice_throwsBusinessException() {
        assertThatThrownBy(() -> rules.checkIfBalanceIsEnough(100, 200))
                .isInstanceOf(BusinessException.class)
                .hasMessage("NOT_ENOUGH_MONEY");
    }

    @Test
    void checkIfCardExists_whenCardDoesNotExist_doesNotThrow() {
        when(repository.existsByCardNumber("1234123412341234")).thenReturn(false);

        rules.checkIfCardExists("1234123412341234");
    }

    @Test
    void checkIfCardExists_whenCardAlreadyExists_throwsBusinessException() {
        when(repository.existsByCardNumber("1234123412341234")).thenReturn(true);

        assertThatThrownBy(() -> rules.checkIfCardExists("1234123412341234"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CARD_NUMBER_ALREADY_EXISTS");
    }

    @Test
    void checkIfPaymentIsValid_whenMatchingPaymentExists_doesNotThrow() {
        var request = new PaymentRequest();
        request.setCardNumber("1234123412341234");
        request.setCardHolder("John Doe");
        request.setCardExpirationYear(2030);
        request.setCardExpirationMonth(12);
        request.setCardCvv("123");

        when(repository.existsByCardNumberAndCardHolderAndCardExpirationYearAndCardExpirationMonthAndCardCvv(
                "1234123412341234", "John Doe", 2030, 12, "123"
        )).thenReturn(true);

        rules.checkIfPaymentIsValid(request);
    }

    @Test
    void checkIfPaymentIsValid_whenNoMatchingPaymentExists_throwsBusinessException() {
        var request = new PaymentRequest();
        request.setCardNumber("1234123412341234");
        request.setCardHolder("John Doe");
        request.setCardExpirationYear(2030);
        request.setCardExpirationMonth(12);
        request.setCardCvv("123");

        when(repository.existsByCardNumberAndCardHolderAndCardExpirationYearAndCardExpirationMonthAndCardCvv(
                "1234123412341234", "John Doe", 2030, 12, "123"
        )).thenReturn(false);

        assertThatThrownBy(() -> rules.checkIfPaymentIsValid(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("NOT_A_VALID_PAYMENT");
    }
}
