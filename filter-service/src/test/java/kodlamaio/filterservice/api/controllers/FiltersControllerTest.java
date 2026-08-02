package kodlamaio.filterservice.api.controllers;

import com.kodlamaio.commonpackage.configuration.exceptions.RestExceptionHandler;
import kodlamaio.filterservice.business.abstracts.FilterService;
import kodlamaio.filterservice.business.dto.responses.GetFilterResponse;
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

@ExtendWith(MockitoExtension.class)
class FiltersControllerTest {

    @Mock private FilterService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FiltersController(service))
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    @Test
    void getAll_returns200() throws Exception {
        when(service.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/filters"))
                .andExpect(status().isOk());
    }

    @Test
    void getById_returns200() throws Exception {
        var id = "filter-id-1";
        when(service.getById(id)).thenReturn(new GetFilterResponse());

        mockMvc.perform(get("/api/filters/{id}", id))
                .andExpect(status().isOk());
    }
}
