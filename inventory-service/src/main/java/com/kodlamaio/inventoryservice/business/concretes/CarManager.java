package com.kodlamaio.inventoryservice.business.concretes;

import com.kodlamaio.commonpackage.events.inventory.CarCreatedEvent;
import com.kodlamaio.commonpackage.events.inventory.CarDeletedEvent;
import com.kodlamaio.commonpackage.utils.dto.CarClientResponse;
import com.kodlamaio.commonpackage.utils.dto.ClientResponse;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import com.kodlamaio.inventoryservice.business.abstracts.CarService;
import com.kodlamaio.inventoryservice.business.dto.requests.create.CreateCarRequest;
import com.kodlamaio.inventoryservice.business.dto.requests.update.UpdateCarRequest;
import com.kodlamaio.inventoryservice.business.dto.responses.create.CreateCarResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetAllCarsResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetCarResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.update.UpdateCarResponse;
import com.kodlamaio.inventoryservice.business.outbox.OutboxRecorder;
import com.kodlamaio.inventoryservice.business.rules.CarBusinessRules;
import com.kodlamaio.inventoryservice.entities.Car;
import com.kodlamaio.inventoryservice.entities.enums.State;
import com.kodlamaio.inventoryservice.repository.CarRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CarManager implements CarService {
    private final CarRepository repository;
    private final ModelMapperService mapperService;
    private final CarBusinessRules rules;
    private final OutboxRecorder outboxRecorder;
    private final TransactionTemplate transactionTemplate;

    @Override
    public List<GetAllCarsResponse> getAll() {
        var cars = repository.findAll();
        var response = cars.stream().map(car -> mapperService.forResponse().map(car, GetAllCarsResponse.class)).toList();
        return response;
    }

    @Override
    public GetCarResponse getById(UUID id) {
        rules.checkIfCarExists(id);
        var car = repository.findById(id).orElseThrow();
        var response = mapperService.forResponse().map(car, GetCarResponse.class);

        return response;
    }

    @Override
    public CarClientResponse getCarForInvoice(UUID id) {
        rules.checkIfCarExists(id);
        var car = repository.findById(id).orElseThrow();
        var response = mapperService.forResponse().map(car, CarClientResponse.class);

        return response;
    }

    @Override
    public CreateCarResponse add(CreateCarRequest request) {
        var car = mapperService.forRequest().map(request, Car.class);
        car.setId(UUID.randomUUID());
        car.setState(State.Available);
        var createdCar = transactionTemplate.execute(status -> {
            var savedCar = repository.save(car);
            recordCarCreatedEvent(savedCar);
            return savedCar;
        });

        var response = mapperService.forResponse().map(createdCar, CreateCarResponse.class);

        return response;
    }

    @Override
    public UpdateCarResponse update(UUID id, UpdateCarRequest request) {
        rules.checkIfCarExists(id);
        var car = mapperService.forRequest().map(request, Car.class);
        car.setId(id);
        repository.save(car);
        var response = mapperService.forResponse().map(car, UpdateCarResponse.class);
        return response;
    }

    @Override
    public void delete(UUID id) {
        rules.checkIfCarExists(id);
        transactionTemplate.executeWithoutResult(status -> {
            recordCarDeletedEvent(id);
            repository.deleteById(id);
        });
    }

    @Override
    public ClientResponse checkIfCarAvailable(UUID id) {
        var response = new ClientResponse();
        validateCarAvailability(id, response);

        return response;
    }

    private void validateCarAvailability(UUID id, ClientResponse response)
    {
        try
        {
            rules.checkIfCarExists(id);
            rules.checkCarAvailability(id);
            response.setSuccess(true);
        } catch (BusinessException exception)
        {
            response.setSuccess(false);
            response.setMessage(exception.getMessage());
        }
    }

    @Override
    public void changeStateByCarId(State state, UUID id) {
        repository.changeStateByCarId(state, id);
    }

    private void recordCarCreatedEvent(Car createdCar)
    {
        var event = mapperService.forResponse().map(createdCar, CarCreatedEvent.class);
        outboxRecorder.record(event, "car-created");
    }

    private void recordCarDeletedEvent(UUID id)
    {
        outboxRecorder.record(new CarDeletedEvent(id), "car-deleted");
    }
}