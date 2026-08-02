package com.kodlamaio.invoiceservice.business.kafka.consumer;

import com.kodlamaio.commonpackage.events.rentalPayment.RentalPaymentCreatedEvent;
import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import com.kodlamaio.invoiceservice.business.abstracts.InvoiceService;
import com.kodlamaio.invoiceservice.entities.Invoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalConsumerTest {

    @Mock private InvoiceService service;
    @Mock private ModelMapperService mapper;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private RentalConsumer consumer;

    @Test
    void consume_rentalPaymentCreatedEvent_mapsEventToInvoiceAndSavesIt() {
        var event = new RentalPaymentCreatedEvent();
        var invoice = new Invoice();

        when(mapper.forRequest()).thenReturn(modelMapper);
        when(modelMapper.map(event, Invoice.class)).thenReturn(invoice);

        consumer.consume(event);

        verify(service).add(invoice);
    }
}
