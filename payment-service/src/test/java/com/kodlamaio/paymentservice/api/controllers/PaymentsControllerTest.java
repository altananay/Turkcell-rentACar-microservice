package com.kodlamaio.paymentservice.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodlamaio.commonpackage.configuration.exceptions.RestExceptionHandler;
import com.kodlamaio.commonpackage.utils.dto.ClientResponse;
import com.kodlamaio.commonpackage.utils.dto.CreateRentalPaymentRequest;
import com.kodlamaio.paymentservice.business.abstracts.PaymentService;
import com.kodlamaio.paymentservice.business.dto.requests.CreatePaymentRequest;
import com.kodlamaio.paymentservice.business.dto.responses.CreatePaymentResponse;
import com.kodlamaio.paymentservice.business.dto.responses.GetPaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentsControllerTest {

    @Mock private PaymentService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentsController(service))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void getAll_returns200() throws Exception {
        when(service.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_returns200() throws Exception {
        var id = UUID.randomUUID();
        when(service.getById(id)).thenReturn(new GetPaymentResponse());

        mockMvc.perform(get("/api/payments/{id}", id))
                .andExpect(status().isOk());
    }

    // PaymentsController#add now carries @ResponseStatus(CREATED), matching every other
    // service's add endpoint.
    @Test
    void add_withValidRequest_returns201() throws Exception {
        var request = new CreatePaymentRequest();
        request.setCardNumber("1234123412341234");
        request.setCardHolder("John Doe");
        request.setCardExpirationYear(2030);
        request.setCardExpirationMonth(12);
        request.setCardCvv("123");
        request.setBalance(100);

        when(service.add(any(CreatePaymentRequest.class))).thenReturn(new CreatePaymentResponse());

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void add_withInvalidRequest_returns400() throws Exception {
        // blank cardNumber/cardHolder/cardCvv violate @NotBlank/@Length — @Valid IS present on add.
        var request = new CreatePaymentRequest();

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // PaymentsController#delete now carries @ResponseStatus(NO_CONTENT), matching every other
    // service's delete endpoint.
    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/payments/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }

    // processRentalPayment's @RequestBody has no @Valid, so even an empty/invalid body reaches the
    // service and still returns 200 — this documents the missing validation, not desired behavior.
    @Test
    void processRentalPayment_withInvalidBody_stillReturns200() throws Exception {
        when(service.processRentalPayment(anyString(), any(CreateRentalPaymentRequest.class)))
                .thenReturn(new ClientResponse());

        mockMvc.perform(post("/api/payments/process-rental-payment")
                        .header("Idempotency-Key", "saga-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void processRentalPayment_withMissingIdempotencyKeyHeader_returns400() throws Exception {
        mockMvc.perform(post("/api/payments/process-rental-payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
