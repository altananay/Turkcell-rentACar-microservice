package kodlamaio.filterservice.api.controllers;

import jakarta.annotation.PostConstruct;
import kodlamaio.filterservice.business.abstracts.FilterService;
import kodlamaio.filterservice.business.dto.responses.GetAllFiltersResponse;
import kodlamaio.filterservice.business.dto.responses.GetFilterResponse;
import kodlamaio.filterservice.entities.Filter;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/filters")
public class FiltersController {

    private final FilterService service;

    /*@PostConstruct
    public void createDb()
    {
        service.add(new Filter());
    }*/

    @GetMapping
    public List<GetAllFiltersResponse> getAll()
    {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public GetFilterResponse getById(@PathVariable UUID id)
    {
        return service.getById(id);
    }
}