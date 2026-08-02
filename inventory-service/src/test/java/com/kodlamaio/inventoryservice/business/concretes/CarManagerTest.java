package com.kodlamaio.inventoryservice.business.concretes;

import com.kodlamaio.commonpackage.events.inventory.CarCreatedEvent;
import com.kodlamaio.commonpackage.events.inventory.CarDeletedEvent;
import com.kodlamaio.commonpackage.kafka.producer.KafkaProducer;
import com.kodlamaio.commonpackage.utils.dto.ClientResponse;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import com.kodlamaio.inventoryservice.business.dto.requests.create.CreateCarRequest;
import com.kodlamaio.inventoryservice.business.dto.responses.create.CreateCarResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetAllCarsResponse;
import com.kodlamaio.inventoryservice.business.dto.responses.get.GetCarResponse;
import com.kodlamaio.inventoryservice.business.rules.CarBusinessRules;
import com.kodlamaio.inventoryservice.entities.Car;
import com.kodlamaio.inventoryservice.entities.enums.State;
import com.kodlamaio.inventoryservice.repository.CarRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarManagerTest {

    @Mock private CarRepository repository;
    @Mock private ModelMapperService mapperService;
    @Mock private CarBusinessRules rules;
    @Mock private KafkaProducer producer;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private CarManager carManager;

    @Test
    void getAll_mapsEveryCarToResponse() {
        var car1 = new Car();
        var car2 = new Car();
        var resp1 = new GetAllCarsResponse();
        var resp2 = new GetAllCarsResponse();

        when(repository.findAll()).thenReturn(List.of(car1, car2));
        when(mapperService.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(car1, GetAllCarsResponse.class)).thenReturn(resp1);
        when(modelMapper.map(car2, GetAllCarsResponse.class)).thenReturn(resp2);

        var result = carManager.getAll();

        assertThat(result).containsExactly(resp1, resp2);
    }

    @Test
    void getById_whenCarExists_returnsMappedResponse() {
        var carId = UUID.randomUUID();
        var car = new Car(carId, 2023, "34 ABC 123", State.Available, 100.0, null);
        var expected = new GetCarResponse();

        when(repository.findById(carId)).thenReturn(Optional.of(car));
        when(mapperService.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(car, GetCarResponse.class)).thenReturn(expected);

        var response = carManager.getById(carId);

        assertThat(response).isSameAs(expected);
        verify(rules).checkIfCarExists(carId);
    }

    @Test
    void getById_whenRulesReject_propagatesBusinessException() {
        var carId = UUID.randomUUID();
        doThrow(new BusinessException("CAR_NOT_EXISTS")).when(rules).checkIfCarExists(carId);

        assertThrows(BusinessException.class, () -> carManager.getById(carId));
        verifyNoInteractions(repository);
    }

    @Test
    void add_assignsIdAndAvailableState_savesAndPublishesCarCreatedEvent() {
        var request = new CreateCarRequest();
        var mappedCar = new Car();
        var createdEvent = new CarCreatedEvent();
        var response = new CreateCarResponse();

        when(mapperService.forRequest()).thenReturn(modelMapper);
        when(modelMapper.map(request, Car.class)).thenReturn(mappedCar);
        when(repository.save(any(Car.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapperService.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(any(Car.class), eq(CarCreatedEvent.class))).thenReturn(createdEvent);
        when(modelMapper.map(any(Car.class), eq(CreateCarResponse.class))).thenReturn(response);

        var result = carManager.add(request);

        var carCaptor = ArgumentCaptor.forClass(Car.class);
        verify(repository).save(carCaptor.capture());
        assertThat(carCaptor.getValue().getId()).isNotNull();
        assertThat(carCaptor.getValue().getState()).isEqualTo(State.Available);

        verify(producer).sendMessage(createdEvent, "car-created");
        assertThat(result).isSameAs(response);
    }

    @Test
    void delete_whenCarExists_deletesAndPublishesCarDeletedEvent() {
        var carId = UUID.randomUUID();

        carManager.delete(carId);

        verify(rules).checkIfCarExists(carId);
        verify(repository).deleteById(carId);

        var eventCaptor = ArgumentCaptor.forClass(CarDeletedEvent.class);
        verify(producer).sendMessage(eventCaptor.capture(), eq("car-deleted"));
        assertThat(eventCaptor.getValue().getCarId()).isEqualTo(carId);
    }

    @Test
    void checkIfCarAvailable_whenRulesPass_returnsSuccess() {
        var carId = UUID.randomUUID();

        ClientResponse response = carManager.checkIfCarAvailable(carId);

        assertThat(response.isSuccess()).isTrue();
        verify(rules).checkIfCarExists(carId);
        verify(rules).checkCarAvailability(carId);
    }

    @Test
    void checkIfCarAvailable_whenCarNotAvailable_returnsFailureWithRuleMessage() {
        var carId = UUID.randomUUID();
        doThrow(new BusinessException("CAR_NOT_AVAILABLE")).when(rules).checkCarAvailability(carId);

        ClientResponse response = carManager.checkIfCarAvailable(carId);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("CAR_NOT_AVAILABLE");
    }
}
