package com.kodlamaio.inventoryservice.business.kafka.consumer;

import com.kodlamaio.commonpackage.events.maintenance.MaintenanceCreatedEvent;
import com.kodlamaio.commonpackage.events.maintenance.MaintenanceDeletedEvent;
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
class MaintenanceConsumerTest {

    @Mock private CarService service;

    @InjectMocks
    private MaintenanceConsumer consumer;

    @Test
    void consume_maintenanceCreatedEvent_marksCarUnderMaintenance() {
        var carId = UUID.randomUUID();

        consumer.consume(new MaintenanceCreatedEvent(carId));

        verify(service).changeStateByCarId(State.Maintenance, carId);
    }

    @Test
    void consume_maintenanceDeletedEvent_marksCarAvailable() {
        var carId = UUID.randomUUID();

        consumer.consume(new MaintenanceDeletedEvent(carId));

        verify(service).changeStateByCarId(State.Available, carId);
    }
}
