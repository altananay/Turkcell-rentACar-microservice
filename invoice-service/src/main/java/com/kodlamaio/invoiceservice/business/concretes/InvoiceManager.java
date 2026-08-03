package com.kodlamaio.invoiceservice.business.concretes;

import com.kodlamaio.commonpackage.kafka.producer.KafkaProducer;
import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import com.kodlamaio.invoiceservice.business.abstracts.InvoiceService;
import com.kodlamaio.invoiceservice.business.dto.responses.GetAllInvoicesResponse;
import com.kodlamaio.invoiceservice.business.dto.responses.GetInvoiceResponse;
import com.kodlamaio.invoiceservice.business.rules.InvoiceBusinessRules;
import com.kodlamaio.invoiceservice.entities.Invoice;
import com.kodlamaio.invoiceservice.repository.InvoiceRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class InvoiceManager implements InvoiceService {
    private final InvoiceRepository repository;
    private final ModelMapperService mapper;
    private final InvoiceBusinessRules rules;
    private final KafkaProducer producer;

    @Override
    public List<GetAllInvoicesResponse> getAll() {
        List<Invoice> invoices = repository.findAll();
        List<GetAllInvoicesResponse> response =
                invoices.stream().map(invoice -> mapper.forResponse().map(invoice, GetAllInvoicesResponse.class)).toList();
        return response;
    }

    @Override
    public GetInvoiceResponse getById(String id) {
        rules.checkIfInvoiceExists(id);
        Invoice invoice = repository.findById(id).get();
        GetInvoiceResponse response = mapper.forResponse().map(invoice, GetInvoiceResponse.class);
        return response;
    }

    @Override
    public void add(Invoice invoice) {
        // A redelivered rental-payment-created event would otherwise insert a second invoice for the
        // same rental. The null guard is load-bearing, not defensive padding: Mongo matches a null
        // rentalId against every invoice written before the field existed.
        if (Objects.nonNull(invoice.getRentalId())) {
            repository.findFirstByRentalId(invoice.getRentalId())
                    .ifPresent(existing -> invoice.setId(existing.getId()));
        }
        repository.save(invoice);
    }
}