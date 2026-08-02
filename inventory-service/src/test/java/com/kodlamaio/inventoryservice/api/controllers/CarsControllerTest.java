package com.kodlamaio.inventoryservice.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodlamaio.commonpackage.configuration.exceptions.RestExceptionHandler;
import com.kodlamaio.inventoryservice.business.abstracts.CarService;
import com.kodlamaio.inventoryservice.business.dto.requests.create.CreateCarRequest;
import com.kodlamaio.inventoryservice.business.dto.responses.create.CreateCarResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetCarResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// NOTE: @Secured("ROLE_admin") on getAll() is a Spring AOP proxy backed by
// @EnableMethodSecurity — it requires a full Spring context and is NOT enforced
// here. This class tests request mapping, validation, and status codes only.
@ExtendWith(MockitoExtension.class)
class CarsControllerTest {

    @Mock private CarService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CarsController(service))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void getById_returns200() throws Exception {
        var carId = UUID.randomUUID();
        when(service.getById(carId)).thenReturn(new GetCarResponse());

        mockMvc.perform(get("/api/cars/{id}", carId))
                .andExpect(status().isOk());
    }

    @Test
    void add_withValidRequest_returns201() throws Exception {
        var request = new CreateCarRequest();
        request.setModelId(UUID.randomUUID());
        request.setModelYear(2023);
        request.setPlate("34 ABC 1234");
        request.setDailyPrice(150.0);

        when(service.add(any(CreateCarRequest.class))).thenReturn(new CreateCarResponse());

        mockMvc.perform(post("/api/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void add_withInvalidPlate_returns400() throws Exception {
        var request = new CreateCarRequest();
        request.setModelId(UUID.randomUUID());
        request.setModelYear(2023);
        request.setPlate("INVALID-PLATE");
        request.setDailyPrice(150.0);

        mockMvc.perform(post("/api/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void add_withMissingModelId_returns400() throws Exception {
        var request = new CreateCarRequest();
        request.setModelYear(2023);
        request.setPlate("34 ABC 1234");
        request.setDailyPrice(150.0);

        mockMvc.perform(post("/api/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/cars/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
