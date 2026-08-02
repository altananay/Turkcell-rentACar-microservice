package com.kodlamaio.inventoryservice.business.kafka.consumer;

import com.kodlamaio.commonpackage.events.rental.RentalCreatedEvent;
import com.kodlamaio.commonpackage.events.rental.RentalDeletedEvent;
import com.kodlamaio.inventoryservice.business.abstracts.CarService;
import com.kodlamaio.inventoryservice.entities.enums.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RentalConsumerTest {

    @Mock private CarService service;

    @InjectMocks
    private RentalConsumer consumer;

    @Test
    void consume_rentalCreatedEvent_marksCarRented() {
        var carId = UUID.randomUUID();

        consumer.consume(new RentalCreatedEvent(carId));

        verify(service).changeStateByCarId(State.Rented, carId);
    }

    @Test
    void consume_rentalDeletedEvent_marksCarAvailable() {
        var carId = UUID.randomUUID();

        consumer.consume(new RentalDeletedEvent(carId));

        verify(service).changeStateByCarId(State.Available, carId);
    }
}
