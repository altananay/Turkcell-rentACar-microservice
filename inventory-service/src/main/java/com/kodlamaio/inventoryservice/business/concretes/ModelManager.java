package com.kodlamaio.inventoryservice.business.concretes;

import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import com.kodlamaio.inventoryservice.business.abstracts.ModelService;
import com.kodlamaio.inventoryservice.business.dto.requests.create.CreateModelRequest;
import com.kodlamaio.inventoryservice.business.dto.requests.update.UpdateModelRequest;
import com.kodlamaio.inventoryservice.business.dto.responses.create.CreateModelResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetAllModelsResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetModelResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.update.UpdateModelResponse;
import com.kodlamaio.inventoryservice.business.rules.ModelBusinessRules;
import com.kodlamaio.inventoryservice.entities.Model;
import com.kodlamaio.inventoryservice.repository.ModelRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class ModelManager implements ModelService {

    private final ModelRepository repository;
    private final ModelMapperService mapperService;
    private final ModelBusinessRules rules;

    @Override
    public List<GetAllModelsResponse> getAll() {
        var models = repository.findAll();
        var responses = models.stream().map(model -> mapperService.forResponse().map(model, GetAllModelsResponse.class)).toList();
        return responses;
    }

    @Override
    public GetModelResponse getById(UUID id) {
        rules.checkIfModelExists(id);
        var model = repository.findById(id).orElseThrow();
        var response = mapperService.forResponse().map(model, GetModelResponse.class);
        return response;
    }

    @Override
    public CreateModelResponse add(CreateModelRequest request) {
        var model = mapperService.forRequest().map(request, Model.class);
        model.setId(null);
        repository.save(model);
        var response = mapperService.forResponse().map(model, CreateModelResponse.class);
        return response;
    }

    @Override
    public UpdateModelResponse update(UUID id, UpdateModelRequest request) {
        rules.checkIfModelExists(id);
        var model = mapperService.forRequest().map(request, Model.class);
        model.setId(id);
        repository.save(model);
        var response = mapperService.forResponse().map(model, UpdateModelResponse.class);
        return response;
    }

    @Override
    public void delete(UUID id) {
        rules.checkIfModelExists(id);
        repository.deleteById(id);
    }
}