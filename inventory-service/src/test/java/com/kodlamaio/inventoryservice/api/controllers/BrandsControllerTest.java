package com.kodlamaio.inventoryservice.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kodlamaio.commonpackage.configuration.exceptions.RestExceptionHandler;
import com.kodlamaio.inventoryservice.business.abstracts.BrandService;
import com.kodlamaio.inventoryservice.business.dto.requests.create.CreateBrandRequest;
import com.kodlamaio.inventoryservice.business.dto.requests.update.UpdateBrandRequest;
import com.kodlamaio.inventoryservice.business.dto.responses.create.CreateBrandResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetBrandResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.update.UpdateBrandResponse;
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
class BrandsControllerTest {

    @Mock private BrandService service;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BrandsController(service))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void getById_returns200() throws Exception {
        var brandId = UUID.randomUUID();
        when(service.getById(brandId)).thenReturn(new GetBrandResponse());

        mockMvc.perform(get("/api/brands/{id}", brandId))
                .andExpect(status().isOk());
    }

    @Test
    void add_withValidRequest_returns201() throws Exception {
        var request = new CreateBrandRequest();
        request.setName("Toyota");

        when(service.add(any(CreateBrandRequest.class))).thenReturn(new CreateBrandResponse());

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void add_withBlankName_returns400() throws Exception {
        var request = new CreateBrandRequest();
        request.setName("");

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void add_withTooShortName_returns400() throws Exception {
        var request = new CreateBrandRequest();
        request.setName("A");

        mockMvc.perform(post("/api/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // UpdateBrandRequest.id was previously @NotBlank on a UUID field, which made
    // Hibernate Validator throw UnexpectedTypeException on every update. Fixed to
    // @NotNull — this now validates and reaches the service normally.
    @Test
    void update_withValidRequest_returns200() throws Exception {
        var id = UUID.randomUUID();
        var request = new UpdateBrandRequest();
        request.setId(id);
        request.setName("Toyota");

        when(service.update(eq(id), any(UpdateBrandRequest.class))).thenReturn(new UpdateBrandResponse());

        mockMvc.perform(put("/api/brands/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns204() throws Exception {
        mockMvc.perform(delete("/api/brands/{id}", UUID.randomUUID()))
                .andExpect(status().isNoContent());
    }
}
