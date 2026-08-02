package kodlamaio.filterservice.business.kafka.consumer;

import com.kodlamaio.commonpackage.events.rental.RentalCreatedEvent;
import com.kodlamaio.commonpackage.events.rental.RentalDeletedEvent;
import kodlamaio.filterservice.business.abstracts.FilterService;
import kodlamaio.filterservice.entities.Filter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalConsumerTest {

    @Mock private FilterService service;

    @InjectMocks
    private RentalConsumer consumer;

    @Test
    void consume_rentalCreatedEvent_setsStateRentedAndSavesFilter() {
        var carId = UUID.randomUUID();
        var event = new RentalCreatedEvent(carId);
        var filter = new Filter();

        when(service.getByCarId(carId)).thenReturn(filter);

        consumer.consume(event);

        var captor = ArgumentCaptor.forClass(Filter.class);
        verify(service).add(captor.capture());
        assertThat(captor.getValue()).isSameAs(filter);
        assertThat(captor.getValue().getState()).isEqualTo("Rented");
    }

    @Test
    void consume_rentalDeletedEvent_setsStateAvailableAndSavesFilter() {
        var carId = UUID.randomUUID();
        var event = new RentalDeletedEvent(carId);
        var filter = new Filter();

        when(service.getByCarId(carId)).thenReturn(filter);

        consumer.consume(event);

        var captor = ArgumentCaptor.forClass(Filter.class);
        verify(service).add(captor.capture());
        assertThat(captor.getValue()).isSameAs(filter);
        assertThat(captor.getValue().getState()).isEqualTo("Available");
    }

    @Test
    void consume_rentalCreatedEvent_whenGetByCarIdReturnsNull_skipsWithoutThrowing() {
        // service.getByCarId can return null (no filter indexed for that car yet, e.g. a
        // race with the car-created event). consume() now guards against this and skips
        // instead of NPEing.
        var carId = UUID.randomUUID();
        var event = new RentalCreatedEvent(carId);

        when(service.getByCarId(carId)).thenReturn(null);

        consumer.consume(event);

        verify(service, never()).add(any());
    }
}
