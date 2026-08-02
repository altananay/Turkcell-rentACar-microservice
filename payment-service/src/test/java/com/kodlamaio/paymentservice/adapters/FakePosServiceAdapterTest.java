package com.kodlamaio.paymentservice.adapters;

import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

// No @ExtendWith(MockitoExtension.class) needed here — mockConstruction is used directly via
// try-with-resources rather than @Mock annotations, so a plain JUnit 5 test class is sufficient.
// Boot 3.1.0 -> Mockito 5.3.1 bundles the inline mock maker, so mockConstruction(Random.class)
// works with zero added dependency.
class FakePosServiceAdapterTest {

    @Test
    void pay_whenRandomReturnsTrue_doesNotThrow() {
        try (var mocked = mockConstruction(Random.class,
                (mock, context) -> when(mock.nextBoolean()).thenReturn(true))) {
            var adapter = new FakePosServiceAdapter();
            assertDoesNotThrow(adapter::pay);
        }
    }

    @Test
    void pay_whenRandomReturnsFalse_throwsBusinessExceptionWithPaymentFailedMessage() {
        try (var mocked = mockConstruction(Random.class,
                (mock, context) -> when(mock.nextBoolean()).thenReturn(false))) {
            var adapter = new FakePosServiceAdapter();
            assertThatThrownBy(adapter::pay)
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("PAYMENT_FAILED");
        }
    }
}
