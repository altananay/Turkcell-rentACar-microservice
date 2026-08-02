package com.kodlamaio.inventoryservice.business.concretes;

import com.kodlamaio.commonpackage.events.inventory.BrandDeletedEvent;
import com.kodlamaio.commonpackage.kafka.producer.KafkaProducer;
import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import com.kodlamaio.inventoryservice.business.dto.requests.create.CreateBrandRequest;
import com.kodlamaio.inventoryservice.business.dto.requests.update.UpdateBrandRequest;
import com.kodlamaio.inventoryservice.business.dto.responses.create.CreateBrandResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetAllBrandsResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetBrandResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.update.UpdateBrandResponse;
import com.kodlamaio.inventoryservice.business.rules.BrandBusinessRules;
import com.kodlamaio.inventoryservice.entities.Brand;
import com.kodlamaio.inventoryservice.repository.BrandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandManagerTest {

    @Mock private BrandRepository repository;
    @Mock private ModelMapperService mapperService;
    @Mock private BrandBusinessRules rules;
    @Mock private KafkaProducer producer;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private BrandManager brandManager;

    @Test
    void getAll_mapsEveryBrandToResponse() {
        var brand1 = new Brand();
        var brand2 = new Brand();
        var resp1 = new GetAllBrandsResponse();
        var resp2 = new GetAllBrandsResponse();

        when(repository.findAll()).thenReturn(List.of(brand1, brand2));
        when(mapperService.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(brand1, GetAllBrandsResponse.class)).thenReturn(resp1);
        when(modelMapper.map(brand2, GetAllBrandsResponse.class)).thenReturn(resp2);

        var result = brandManager.getAll();

        assertThat(result).containsExactly(resp1, resp2);
    }

    @Test
    void getById_whenBrandExists_returnsMappedResponse() {
        var brandId = UUID.randomUUID();
        var brand = new Brand();
        brand.setId(brandId);
        var expected = new GetBrandResponse();

        when(repository.findById(brandId)).thenReturn(Optional.of(brand));
        when(mapperService.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(brand, GetBrandResponse.class)).thenReturn(expected);

        var response = brandManager.getById(brandId);

        assertThat(response).isSameAs(expected);
        verify(rules).checkIfBrandExists(brandId);
    }

    @Test
    void add_mapsSavesAndReturnsResponse() {
        var request = new CreateBrandRequest();
        var mappedBrand = new Brand();
        var response = new CreateBrandResponse();

        when(mapperService.forRequest()).thenReturn(modelMapper);
        when(modelMapper.map(request, Brand.class)).thenReturn(mappedBrand);
        when(mapperService.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(mappedBrand, CreateBrandResponse.class)).thenReturn(response);

        var result = brandManager.add(request);

        verify(repository).save(mappedBrand);
        assertThat(result).isSameAs(response);
    }

    @Test
    void update_whenBrandExists_setsIdSavesAndReturnsResponse() {
        var brandId = UUID.randomUUID();
        var request = new UpdateBrandRequest();
        var mappedBrand = new Brand();
        var response = new UpdateBrandResponse();

        when(mapperService.forRequest()).thenReturn(modelMapper);
        when(modelMapper.map(request, Brand.class)).thenReturn(mappedBrand);
        when(mapperService.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(mappedBrand, UpdateBrandResponse.class)).thenReturn(response);

        var result = brandManager.update(brandId, request);

        verify(rules).checkIfBrandExists(brandId);
        assertThat(mappedBrand.getId()).isEqualTo(brandId);
        verify(repository).save(mappedBrand);
        assertThat(result).isSameAs(response);
    }

    @Test
    void delete_whenBrandExists_deletesAndPublishesBrandDeletedEvent() {
        var brandId = UUID.randomUUID();

        brandManager.delete(brandId);

        verify(rules).checkIfBrandExists(brandId);
        verify(repository).deleteById(brandId);

        var eventCaptor = ArgumentCaptor.forClass(BrandDeletedEvent.class);
        verify(producer).sendMessage(eventCaptor.capture(), eq("brand-deleted"));
        assertThat(eventCaptor.getValue().getBrandId()).isEqualTo(brandId);
    }
}
