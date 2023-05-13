package kodlamaio.filterservice.business.kafka.consumer;

import com.kodlamaio.commonpackage.events.BrandDeletedEvent;
import com.kodlamaio.commonpackage.events.CarCreatedEvent;
import com.kodlamaio.commonpackage.events.CarDeletedEvent;
import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import kodlamaio.filterservice.business.abstracts.FilterService;
import kodlamaio.filterservice.entities.Filter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class InventoryConsumer {
    private final FilterService service;
    private final ModelMapperService mapperService;

    @KafkaListener(topics = "car-created", groupId = "car-create")
    public void consume(CarCreatedEvent event)
    {
        var filter = mapperService.forRequest().map(event, Filter.class);
        service.add(filter);
        log.info("Car created event consumed {}",event);
    }

    @KafkaListener(topics = "car-deleted", groupId = "car-delete")
    public void consume(CarDeletedEvent event)
    {
        service.deleteByCarId(event.getCarId());
        log.info("Car deleted event consume {}",event);
    }

    @KafkaListener(topics = "brand-deleted", groupId = "brand-delete")
    public void consume(BrandDeletedEvent event)
    {
        service.deleteAllByBrandId(event.getBrandId());
        log.info("Brand deleted event consumed {}",event);
    }
}