package com.kodlamaio.inventoryservice.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodlamaio.commonpackage.configuration.exceptions.RestExceptionHandler;
import com.kodlamaio.inventoryservice.business.abstracts.ModelService;
import com.kodlamaio.inventoryservice.business.dto.requests.create.CreateModelRequest;
import com.kodlamaio.inventoryservice.business.dto.requests.update.UpdateModelRequest;
import com.kodlamaio.inventoryservice.business.dto.responses.create.CreateModelResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetModelResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.update.UpdateModelResponse;
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

@ExtendWith(MockitoExtension.class)
class ModelsControllerTest {

    @Mock private ModelService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ModelsController(service))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void getById_returns200() throws Exception {
        var modelId = UUID.randomUUID();
        when(service.getById(modelId)).thenReturn(new GetModelResponse());

        mockMvc.perform(get("/api/models/{id}", modelId))
                .andExpect(status().isOk());
    }

    @Test
    void add_withValidRequest_returns201() throws Exception {
        var request = new CreateModelRequest();
        request.setBrandId(UUID.randomUUID());
        request.setName("Corolla");

        when(service.add(any(CreateModelRequest.class))).thenReturn(new CreateModelResponse());

        mockMvc.perform(post("/api/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void add_withMissingBrandId_returns400() throws Exception {
        var request = new CreateModelRequest();
        request.setName("Corolla");

        mockMvc.perform(post("/api/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void add_withBlankName_returns400() throws Exception {
        var request = new CreateModelRequest();
        request.setBrandId(UUID.randomUUID());
        request.setName("");

        mockMvc.perform(post("/api/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_withValidRequest_returns200() throws Exception {
        var modelId = UUID.randomUUID();
        var request = new UpdateModelRequest();
        request.setBrandId(UUID.randomUUID());
        request.setName("Corolla");

        when(service.update(any(UUID.class), any(UpdateModelRequest.class))).thenReturn(new UpdateModelResponse());

        mockMvc.perform(put("/api/models/{id}", modelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void update_withMissingBrandId_returns400() throws Exception {
        var modelId = UUID.randomUUID();
        var request = new UpdateModelRequest();
        request.setName("Corolla");

        mockMvc.perform(put("/api/models/{id}", modelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/models/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
