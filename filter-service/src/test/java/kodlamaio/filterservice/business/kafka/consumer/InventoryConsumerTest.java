package kodlamaio.filterservice.business.kafka.consumer;

import com.kodlamaio.commonpackage.events.inventory.BrandDeletedEvent;
import com.kodlamaio.commonpackage.events.inventory.CarCreatedEvent;
import com.kodlamaio.commonpackage.events.inventory.CarDeletedEvent;
import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import kodlamaio.filterservice.business.abstracts.FilterService;
import kodlamaio.filterservice.entities.Filter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryConsumerTest {

    @Mock private FilterService service;
    @Mock private ModelMapperService mapperService;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private InventoryConsumer consumer;

    @Test
    void consume_carCreatedEvent_mapsEventToFilterAndAddsIt() {
        var event = new CarCreatedEvent();
        var filter = new Filter();

        when(mapperService.forRequest()).thenReturn(modelMapper);
        when(modelMapper.map(event, Filter.class)).thenReturn(filter);

        consumer.consume(event);

        verify(service).add(filter);
    }

    @Test
    void consume_carDeletedEvent_deletesByCarId() {
        var carId = UUID.randomUUID();
        var event = new CarDeletedEvent(carId);

        consumer.consume(event);

        verify(service).deleteByCarId(carId);
    }

    @Test
    void consume_brandDeletedEvent_deletesAllByBrandId() {
        var brandId = UUID.randomUUID();
        var event = new BrandDeletedEvent(brandId);

        consumer.consume(event);

        verify(service).deleteAllByBrandId(brandId);
    }
}
