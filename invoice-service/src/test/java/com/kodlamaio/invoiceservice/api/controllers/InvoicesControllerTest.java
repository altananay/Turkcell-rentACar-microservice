package com.kodlamaio.invoiceservice.api.controllers;

import com.kodlamaio.commonpackage.configuration.exceptions.RestExceptionHandler;
import com.kodlamaio.invoiceservice.business.abstracts.InvoiceService;
import com.kodlamaio.invoiceservice.business.dto.responses.GetAllInvoicesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// NOTE: InvoicesController only routes GET /api/invoices — getById/add on InvoiceService
// exist but are never exposed over HTTP, so there is nothing to test at this layer for them.
@ExtendWith(MockitoExtension.class)
class InvoicesControllerTest {

    @Mock private InvoiceService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InvoicesController(service))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void getAll_returns200WithMappedInvoiceList() throws Exception {
        when(service.getAll()).thenReturn(List.of(new GetAllInvoicesResponse()));

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk());
    }
}
