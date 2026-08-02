package com.kodlamaio.rentalservice.business.concretes;

import com.kodlamaio.commonpackage.events.rental.RentalDeletedEvent;
import com.kodlamaio.commonpackage.kafka.producer.KafkaProducer;
import com.kodlamaio.commonpackage.utils.dto.PaymentRequest;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import com.kodlamaio.rentalservice.business.dto.requests.CreateRentalRequest;
import com.kodlamaio.rentalservice.business.dto.requests.UpdateRentalRequest;
import com.kodlamaio.rentalservice.business.dto.responses.CreateRentalResponse;
import com.kodlamaio.rentalservice.business.dto.responses.GetAllRentalsResponse;
import com.kodlamaio.rentalservice.business.dto.responses.GetRentalResponse;
import com.kodlamaio.rentalservice.business.dto.responses.UpdateRentalResponse;
import com.kodlamaio.rentalservice.business.rules.RentalBusinessRules;
import com.kodlamaio.rentalservice.business.saga.RentalCreationSagaOrchestrator;
import com.kodlamaio.rentalservice.entities.Rental;
import com.kodlamaio.rentalservice.repository.RentalRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalManagerTest {

    @Mock private RentalRepository repository;
    @Mock private ModelMapperService mapper;
    @Mock private RentalBusinessRules rules;
    @Mock private KafkaProducer producer;
    @Mock private RentalCreationSagaOrchestrator sagaOrchestrator;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private RentalManager rentalManager;

    @Test
    void getAll_mapsEveryRentalToResponse() {
        var rental1 = new Rental();
        var rental2 = new Rental();
        var resp1 = new GetAllRentalsResponse();
        var resp2 = new GetAllRentalsResponse();

        when(repository.findAll()).thenReturn(List.of(rental1, rental2));
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(rental1, GetAllRentalsResponse.class)).thenReturn(resp1);
        when(modelMapper.map(rental2, GetAllRentalsResponse.class)).thenReturn(resp2);

        var result = rentalManager.getAll();

        assertThat(result).containsExactly(resp1, resp2);
    }

    @Test
    void getById_whenRentalExists_returnsMappedResponse() {
        var rentalId = UUID.randomUUID();
        var rental = new Rental();
        var expected = new GetRentalResponse();

        when(repository.findById(rentalId)).thenReturn(Optional.of(rental));
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(rental, GetRentalResponse.class)).thenReturn(expected);

        var response = rentalManager.getById(rentalId);

        assertThat(response).isSameAs(expected);
        verify(rules).checkIfRentalExists(rentalId);
    }

    @Test
    void getById_whenRulesReject_propagatesBusinessExceptionAndNeverTouchesRepository() {
        var rentalId = UUID.randomUUID();
        doThrow(new BusinessException("RENTAL_NOT_EXISTS")).when(rules).checkIfRentalExists(rentalId);

        assertThatThrownBy(() -> rentalManager.getById(rentalId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("RENTAL_NOT_EXISTS");

        verifyNoInteractions(repository);
    }

    @Test
    void add_whenCarAvailable_delegatesToSagaOrchestratorAndReturnsMappedResponse() {
        var carId = UUID.randomUUID();
        var paymentRequest = new PaymentRequest("1234567890123456", "John Doe", 2025, 6, "123");
        var request = new CreateRentalRequest(carId, 100.0, 3, paymentRequest);

        var rental = new Rental();
        var createResponse = new CreateRentalResponse();

        when(sagaOrchestrator.createRental(request)).thenReturn(rental);
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(rental, CreateRentalResponse.class)).thenReturn(createResponse);

        var result = rentalManager.add(request);

        var inOrder = inOrder(rules, sagaOrchestrator);
        inOrder.verify(rules).ensureCarIsAvailable(carId);
        inOrder.verify(sagaOrchestrator).createRental(request);

        assertThat(result).isSameAs(createResponse);
    }

    @Test
    void add_whenCarNotAvailable_throwsAndNeverCallsOrchestrator() {
        var carId = UUID.randomUUID();
        var request = new CreateRentalRequest(carId, 100.0, 3, new PaymentRequest());
        doThrow(new BusinessException("CAR_NOT_AVAILABLE")).when(rules).ensureCarIsAvailable(carId);

        assertThatThrownBy(() -> rentalManager.add(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("CAR_NOT_AVAILABLE");

        verifyNoInteractions(sagaOrchestrator, repository, producer);
    }

    @Test
    void update_whenRentalExists_updatesAndReturnsResponse() {
        var rentalId = UUID.randomUUID();
        var request = new UpdateRentalRequest(rentalId, UUID.randomUUID(), 120.0, 5);
        var mappedRental = new Rental();
        var response = new UpdateRentalResponse();

        when(mapper.forRequest()).thenReturn(modelMapper);
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(request, Rental.class)).thenReturn(mappedRental);
        when(modelMapper.map(mappedRental, UpdateRentalResponse.class)).thenReturn(response);
        when(repository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = rentalManager.update(rentalId, request);

        verify(rules).checkIfRentalExists(rentalId);
        assertThat(mappedRental.getId()).isEqualTo(rentalId);
        verify(repository).save(mappedRental);
        assertThat(result).isSameAs(response);
    }

    @Test
    void delete_whenRentalExists_publishesEventBeforeDeleting() {
        var rentalId = UUID.randomUUID();
        var carId = UUID.randomUUID();
        var rental = new Rental();
        rental.setId(rentalId);
        rental.setCarId(carId);

        when(repository.findById(rentalId)).thenReturn(Optional.of(rental));

        rentalManager.delete(rentalId);

        verify(rules).checkIfRentalExists(rentalId);

        var eventCaptor = ArgumentCaptor.forClass(RentalDeletedEvent.class);
        var inOrder = inOrder(producer, repository);
        inOrder.verify(producer).sendMessage(eventCaptor.capture(), eq("rental-deleted"));
        inOrder.verify(repository).deleteById(rentalId);

        assertThat(eventCaptor.getValue().getCarId()).isEqualTo(carId);
    }
}
