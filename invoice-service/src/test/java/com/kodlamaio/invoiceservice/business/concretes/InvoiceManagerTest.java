package com.kodlamaio.invoiceservice.business.concretes;

import com.kodlamaio.commonpackage.kafka.producer.KafkaProducer;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import com.kodlamaio.invoiceservice.business.dto.responses.GetAllInvoicesResponse;
import com.kodlamaio.invoiceservice.business.dto.responses.GetInvoiceResponse;
import com.kodlamaio.invoiceservice.business.rules.InvoiceBusinessRules;
import com.kodlamaio.invoiceservice.entities.Invoice;
import com.kodlamaio.invoiceservice.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceManagerTest {

    @Mock private InvoiceRepository repository;
    @Mock private ModelMapperService mapper;
    @Mock private InvoiceBusinessRules rules;
    @Mock private KafkaProducer producer;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private InvoiceManager invoiceManager;

    @Test
    void getAll_mapsEveryInvoiceToResponse() {
        var invoice1 = new Invoice();
        var invoice2 = new Invoice();
        var resp1 = new GetAllInvoicesResponse();
        var resp2 = new GetAllInvoicesResponse();

        when(repository.findAll()).thenReturn(List.of(invoice1, invoice2));
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(invoice1, GetAllInvoicesResponse.class)).thenReturn(resp1);
        when(modelMapper.map(invoice2, GetAllInvoicesResponse.class)).thenReturn(resp2);

        var result = invoiceManager.getAll();

        assertThat(result).containsExactly(resp1, resp2);
    }

    @Test
    void getById_whenInvoiceExists_returnsMappedResponse() {
        var id = UUID.randomUUID().toString();
        var invoice = new Invoice();
        var expected = new GetInvoiceResponse();

        when(repository.findById(id)).thenReturn(Optional.of(invoice));
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(invoice, GetInvoiceResponse.class)).thenReturn(expected);

        var response = invoiceManager.getById(id);

        assertThat(response).isSameAs(expected);
        verify(rules).checkIfInvoiceExists(id);
    }

    @Test
    void getById_whenRulesReject_propagatesBusinessExceptionAndSkipsRepository() {
        var id = UUID.randomUUID().toString();
        doThrow(new BusinessException("INVOICE_NOT_EXISTS")).when(rules).checkIfInvoiceExists(id);

        assertThatThrownBy(() -> invoiceManager.getById(id))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void add_savesInvoice() {
        var invoice = new Invoice();

        invoiceManager.add(invoice);

        verify(repository).save(invoice);
    }
}
