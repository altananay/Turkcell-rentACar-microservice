package kodlamaio.filterservice.business.concretes;

import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import kodlamaio.filterservice.business.dto.responses.GetAllFiltersResponse;
import kodlamaio.filterservice.business.dto.responses.GetFilterResponse;
import kodlamaio.filterservice.entities.Filter;
import kodlamaio.filterservice.repository.FilterRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilterManagerTest {

    @Mock private FilterRepository repository;
    @Mock private ModelMapperService mapper;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private FilterManager filterManager;

    @Test
    void getAll_mapsEveryFilterToResponse() {
        var filter1 = new Filter();
        var filter2 = new Filter();
        var resp1 = new GetAllFiltersResponse();
        var resp2 = new GetAllFiltersResponse();

        when(repository.findAll()).thenReturn(List.of(filter1, filter2));
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(filter1, GetAllFiltersResponse.class)).thenReturn(resp1);
        when(modelMapper.map(filter2, GetAllFiltersResponse.class)).thenReturn(resp2);

        var result = filterManager.getAll();

        assertThat(result).containsExactly(resp1, resp2);
    }

    @Test
    void getById_whenFilterExists_returnsMappedResponse() {
        var id = "filter-id-1";
        var filter = new Filter();
        var expected = new GetFilterResponse();

        when(repository.findById(id)).thenReturn(Optional.of(filter));
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(filter, GetFilterResponse.class)).thenReturn(expected);

        var result = filterManager.getById(id);

        assertThat(result).isSameAs(expected);
    }

    @Test
    void getById_whenFilterMissing_throwsNoSuchElementException() {
        // No rules class exists in this service, so getById calls a bare
        // orElseThrow() with no existence guard — documents the real gap.
        var id = "missing-id";
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> filterManager.getById(id))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void add_savesFilterDirectlyWithoutMapping() {
        var filter = new Filter();

        filterManager.add(filter);

        verify(repository).save(filter);
        verifyNoInteractions(mapper);
    }

    @Test
    void delete_deletesById() {
        var id = "filter-id-1";

        filterManager.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void deleteByCarId_delegatesToRepository() {
        var carId = UUID.randomUUID();

        filterManager.deleteByCarId(carId);

        verify(repository).deleteByCarId(carId);
    }

    @Test
    void deleteAllByBrandId_delegatesToRepository() {
        var brandId = UUID.randomUUID();

        filterManager.deleteAllByBrandId(brandId);

        verify(repository).deleteAllByBrandId(brandId);
    }

    @Test
    void deleteAllByModelId_delegatesToRepository() {
        var modelId = UUID.randomUUID();

        filterManager.deleteAllByModelId(modelId);

        verify(repository).deleteAllByModelId(modelId);
    }

    @Test
    void getByCarId_whenFilterExists_returnsFilter() {
        var carId = UUID.randomUUID();
        var filter = new Filter();

        when(repository.findByCarId(carId)).thenReturn(filter);

        var result = filterManager.getByCarId(carId);

        assertThat(result).isSameAs(filter);
    }

    @Test
    void getByCarId_whenNoFilterExists_returnsNull() {
        // getByCarId has no null-guard in production code — it returns
        // whatever the repository gives it, including a literal null.
        var carId = UUID.randomUUID();

        when(repository.findByCarId(carId)).thenReturn(null);

        var result = filterManager.getByCarId(carId);

        assertThat(result).isNull();
    }
}
