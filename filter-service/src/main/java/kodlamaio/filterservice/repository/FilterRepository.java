package kodlamaio.filterservice.repository;

import kodlamaio.filterservice.entities.Filter;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;
import java.util.UUID;

public interface FilterRepository extends MongoRepository<Filter, String> {
    void deleteByCarId(UUID carId);
    void deleteAllByBrandId(UUID brandId);
    void deleteAllByModelId(UUID modelId);
    // findFirst, not findBy: a single-result finder throws when duplicates already exist, and duplicates
    // are exactly what this collection accumulated before car-created became an upsert.
    Optional<Filter> findFirstByCarIdOrderByIdAsc(UUID carId);
}