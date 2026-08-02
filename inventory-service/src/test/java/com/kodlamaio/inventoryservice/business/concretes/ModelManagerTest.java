package com.kodlamaio.inventoryservice.business.concretes;

import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import com.kodlamaio.inventoryservice.business.dto.requests.create.CreateModelRequest;
import com.kodlamaio.inventoryservice.business.dto.requests.update.UpdateModelRequest;
import com.kodlamaio.inventoryservice.business.dto.responses.create.CreateModelResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetAllModelsResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetModelResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.update.UpdateModelResponse;
import com.kodlamaio.inventoryservice.business.rules.ModelBusinessRules;
import com.kodlamaio.inventoryservice.entities.Model;
import com.kodlamaio.inventoryservice.repository.ModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelManagerTest {

    @Mock private ModelRepository repository;
    @Mock private ModelMapperService mapperService;
    @Mock private ModelBusinessRules rules;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private ModelManager modelManager;

    @Test
    void getAll_mapsEveryModelToResponse() {
        var model1 = new Model();
        var model2 = new Model();
        var resp1 = new GetAllModelsResponse();
        var resp2 = new GetAllModelsResponse();

        when(repository.findAll()).thenReturn(List.of(model1, model2));
        when(mapperService.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(model1, GetAllModelsResponse.class)).thenReturn(resp1);
        when(modelMapper.map(model2, GetAllModelsResponse.class)).thenReturn(resp2);

        var result = modelManager.getAll();

        assertThat(result).containsExactly(resp1, resp2);
    }

    @Test
    void getById_whenModelExists_returnsMappedResponse() {
        var modelId = UUID.randomUUID();
        var model = new Model();
        model.setId(modelId);
        var expected = new GetModelResponse();

        when(repository.findById(modelId)).thenReturn(Optional.of(model));
        when(mapperService.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(model, GetModelResponse.class)).thenReturn(expected);

        var response = modelManager.getById(modelId);

        assertThat(response).isSameAs(expected);
        verify(rules).checkIfModelExists(modelId);
    }

    @Test
    void add_clearsIdBeforeSave_savesAndReturnsResponse() {
        var request = new CreateModelRequest();
        var mappedModel = new Model();
        mappedModel.setId(UUID.randomUUID());
        var response = new CreateModelResponse();

        when(mapperService.forRequest()).thenReturn(modelMapper);
        when(modelMapper.map(request, Model.class)).thenReturn(mappedModel);
        when(mapperService.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(mappedModel, CreateModelResponse.class)).thenReturn(response);

        var result = modelManager.add(request);

        assertThat(mappedModel.getId()).isNull();
        verify(repository).save(mappedModel);
        assertThat(result).isSameAs(response);
    }

    @Test
    void update_whenModelExists_setsIdSavesAndReturnsResponse() {
        var modelId = UUID.randomUUID();
        var request = new UpdateModelRequest();
        var mappedModel = new Model();
        var response = new UpdateModelResponse();

        when(mapperService.forRequest()).thenReturn(modelMapper);
        when(modelMapper.map(request, Model.class)).thenReturn(mappedModel);
        when(mapperService.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(mappedModel, UpdateModelResponse.class)).thenReturn(response);

        var result = modelManager.update(modelId, request);

        verify(rules).checkIfModelExists(modelId);
        assertThat(mappedModel.getId()).isEqualTo(modelId);
        verify(repository).save(mappedModel);
        assertThat(result).isSameAs(response);
    }

    @Test
    void delete_whenModelExists_deletesWithNoKafkaEvent() {
        var modelId = UUID.randomUUID();

        modelManager.delete(modelId);

        verify(rules).checkIfModelExists(modelId);
        verify(repository).deleteById(modelId);
    }
}
