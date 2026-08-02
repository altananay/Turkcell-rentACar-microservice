package com.kodlamaio.maintenanceservice.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodlamaio.commonpackage.configuration.exceptions.RestExceptionHandler;
import com.kodlamaio.maintenanceservice.business.abstracts.MaintenanceService;
import com.kodlamaio.maintenanceservice.business.dto.requests.CreateMaintenanceRequest;
import com.kodlamaio.maintenanceservice.business.dto.responses.CreateMaintenanceResponse;
import com.kodlamaio.maintenanceservice.business.dto.responses.GetAllMaintenancesResponse;
import com.kodlamaio.maintenanceservice.business.dto.responses.GetMaintenanceResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MaintenancesControllerTest {

    @Mock private MaintenanceService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MaintenancesController(service))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void getAll_returns200() throws Exception {
        when(service.getAll()).thenReturn(List.of(new GetAllMaintenancesResponse()));

        mockMvc.perform(get("/api/maintenances"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_returns200() throws Exception {
        var id = UUID.randomUUID();
        when(service.getById(id)).thenReturn(new GetMaintenanceResponse());

        mockMvc.perform(get("/api/maintenances/{id}", id))
                .andExpect(status().isOk());
    }

    @Test
    void add_withValidBody_returns201() throws Exception {
        var request = new CreateMaintenanceRequest();
        request.setCarId(UUID.randomUUID());
        request.setInformation("Oil change");

        when(service.add(any(CreateMaintenanceRequest.class))).thenReturn(new CreateMaintenanceResponse());

        mockMvc.perform(post("/api/maintenances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    // MaintenancesController#add now carries @Valid, matching every other controller in the
    // repo. CreateMaintenanceRequest.carId is @NotNull, so a null carId is now rejected.
    @Test
    void add_withNullCarIdInBody_returns400() throws Exception {
        var request = new CreateMaintenanceRequest();
        request.setCarId(null);
        request.setInformation("Oil change");

        mockMvc.perform(post("/api/maintenances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/maintenances/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
