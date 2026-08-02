package com.kodlamaio.rentalservice.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodlamaio.commonpackage.configuration.exceptions.RestExceptionHandler;
import com.kodlamaio.commonpackage.utils.dto.PaymentRequest;
import com.kodlamaio.rentalservice.business.abstracts.RentalService;
import com.kodlamaio.rentalservice.business.dto.requests.CreateRentalRequest;
import com.kodlamaio.rentalservice.business.dto.requests.UpdateRentalRequest;
import com.kodlamaio.rentalservice.business.dto.responses.CreateRentalResponse;
import com.kodlamaio.rentalservice.business.dto.responses.GetRentalResponse;
import com.kodlamaio.rentalservice.business.dto.responses.UpdateRentalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RentalsControllerTest {

    @Mock private RentalService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RentalsController(service))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    private PaymentRequest validPaymentRequest() {
        return new PaymentRequest("1234567890123456", "John Doe", 2025, 6, "123");
    }

    @Test
    void getAll_returns200() throws Exception {
        mockMvc.perform(get("/api/rentals"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_returns200() throws Exception {
        var rentalId = UUID.randomUUID();
        when(service.getById(rentalId)).thenReturn(new GetRentalResponse());

        mockMvc.perform(get("/api/rentals/{id}", rentalId))
                .andExpect(status().isOk());
    }

    @Test
    void add_withValidRequest_returns201() throws Exception {
        var request = new CreateRentalRequest(UUID.randomUUID(), 100.0, 3, validPaymentRequest());

        when(service.add(any(CreateRentalRequest.class))).thenReturn(new CreateRentalResponse());

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void add_withMissingCarId_returns400() throws Exception {
        var request = new CreateRentalRequest(null, 100.0, 3, validPaymentRequest());

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void add_withDailyPriceBelowMinimum_returns400() throws Exception {
        var request = new CreateRentalRequest(UUID.randomUUID(), 0, 3, validPaymentRequest());

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // Documents an existing gap, not desired behavior: CreateRentalRequest.paymentRequest
    // has no @Valid / @NotNull, so PaymentRequest's own constraints (e.g. @NotBlank cardNumber)
    // are never cascaded by the validator. An empty nested payment request is accepted.
    @Test
    void add_withEmptyNestedPaymentRequest_isStillAcceptedByValidation() throws Exception {
        var request = new CreateRentalRequest(UUID.randomUUID(), 100.0, 3, new PaymentRequest());

        when(service.add(any(CreateRentalRequest.class))).thenReturn(new CreateRentalResponse());

        mockMvc.perform(post("/api/rentals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void update_withValidRequest_returns200() throws Exception {
        var rentalId = UUID.randomUUID();
        var request = new UpdateRentalRequest(rentalId, UUID.randomUUID(), 100.0, 3);

        when(service.update(eq(rentalId), any(UpdateRentalRequest.class))).thenReturn(new UpdateRentalResponse());

        mockMvc.perform(put("/api/rentals/{id}", rentalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/rentals/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
