package kodlamaio.filterservice.business.abstracts;

import kodlamaio.filterservice.business.dto.responses.GetAllFiltersResponse;
import kodlamaio.filterservice.business.dto.responses.GetFilterResponse;
import kodlamaio.filterservice.entities.Filter;

import java.util.List;
import java.util.UUID;

public interface FilterService {
    List<GetAllFiltersResponse> getAll();
    GetFilterResponse getById(UUID id);
    void add(Filter filter);
    void delete(UUID id);
    void deleteAllByBrandId(UUID brandId);
    void deleteAllByModelId(UUID brandId);
}