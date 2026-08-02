package com.kodlamaio.invoiceservice.business.rules;

import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.invoiceservice.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceBusinessRulesTest {

    @Mock private InvoiceRepository repository;

    @InjectMocks
    private InvoiceBusinessRules rules;

    @Test
    void checkIfInvoiceExists_whenInvoiceExists_doesNotThrow() {
        var id = UUID.randomUUID().toString();
        when(repository.existsById(id)).thenReturn(true);

        rules.checkIfInvoiceExists(id);
    }

    @Test
    void checkIfInvoiceExists_whenInvoiceMissing_throwsBusinessException() {
        var id = UUID.randomUUID().toString();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> rules.checkIfInvoiceExists(id))
                .isInstanceOf(BusinessException.class)
                .hasMessage("INVOICE_NOT_EXISTS");
    }
}
