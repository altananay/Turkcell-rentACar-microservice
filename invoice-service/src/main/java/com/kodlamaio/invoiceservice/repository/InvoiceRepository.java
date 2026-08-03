package com.kodlamaio.invoiceservice.repository;

import com.kodlamaio.invoiceservice.entities.Invoice;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends MongoRepository<Invoice, String> {
    // findFirst, not findBy: invoices written before rentalId existed all carry a null one, and a
    // single-result finder would throw as soon as two of them matched.
    Optional<Invoice> findFirstByRentalId(UUID rentalId);
}
