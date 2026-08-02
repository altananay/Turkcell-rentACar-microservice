package com.kodlamaio.maintenanceservice.business.concretes;

import com.kodlamaio.commonpackage.events.maintenance.MaintenanceCreatedEvent;
import com.kodlamaio.commonpackage.events.maintenance.MaintenanceDeletedEvent;
import com.kodlamaio.commonpackage.kafka.producer.KafkaProducer;
import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.commonpackage.utils.mappers.ModelMapperService;
import com.kodlamaio.maintenanceservice.business.dto.requests.CreateMaintenanceRequest;
import com.kodlamaio.maintenanceservice.business.dto.requests.UpdateMaintenanceRequest;
import com.kodlamaio.maintenanceservice.business.dto.responses.CreateMaintenanceResponse;
import com.kodlamaio.maintenanceservice.business.dto.responses.GetAllMaintenancesResponse;
import com.kodlamaio.maintenanceservice.business.dto.responses.GetMaintenanceResponse;
import com.kodlamaio.maintenanceservice.business.dto.responses.UpdateMaintenanceResponse;
import com.kodlamaio.maintenanceservice.business.rules.MaintenanceBusinessRules;
import com.kodlamaio.maintenanceservice.entities.Maintenance;
import com.kodlamaio.maintenanceservice.repository.MaintenanceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
class MaintenanceManagerTest {

    @Mock private MaintenanceRepository repository;
    @Mock private ModelMapperService mapper;
    @Mock private MaintenanceBusinessRules rules;
    @Mock private KafkaProducer producer;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private MaintenanceManager maintenanceManager;

    @Test
    void getAll_mapsEveryMaintenanceToResponse() {
        var maintenance1 = new Maintenance();
        var maintenance2 = new Maintenance();
        var resp1 = new GetAllMaintenancesResponse();
        var resp2 = new GetAllMaintenancesResponse();

        when(repository.findAll()).thenReturn(List.of(maintenance1, maintenance2));
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(maintenance1, GetAllMaintenancesResponse.class)).thenReturn(resp1);
        when(modelMapper.map(maintenance2, GetAllMaintenancesResponse.class)).thenReturn(resp2);

        var result = maintenanceManager.getAll();

        assertThat(result).containsExactly(resp1, resp2);
    }

    @Test
    void getById_whenMaintenanceExists_returnsMappedResponse() {
        var id = UUID.randomUUID();
        var maintenance = new Maintenance();
        var expected = new GetMaintenanceResponse();

        when(repository.findById(id)).thenReturn(Optional.of(maintenance));
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(maintenance, GetMaintenanceResponse.class)).thenReturn(expected);

        var response = maintenanceManager.getById(id);

        assertThat(response).isSameAs(expected);
        verify(rules).checkIfMaintenanceExists(id);
    }

    @Test
    void getById_whenRulesReject_propagatesBusinessException() {
        var id = UUID.randomUUID();
        doThrow(new BusinessException("MAINTENANCE_NOT_EXISTS")).when(rules).checkIfMaintenanceExists(id);

        assertThatThrownBy(() -> maintenanceManager.getById(id))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void add_checksCarUnderMaintenanceThenAvailability_assignsFieldsAndPublishesMaintenanceCreatedEvent() {
        var carId = UUID.randomUUID();
        var request = new CreateMaintenanceRequest();
        request.setCarId(carId);
        var mappedMaintenance = new Maintenance();
        var response = new CreateMaintenanceResponse();

        when(mapper.forRequest()).thenReturn(modelMapper);
        when(modelMapper.map(request, Maintenance.class)).thenReturn(mappedMaintenance);
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(any(Maintenance.class), eq(CreateMaintenanceResponse.class))).thenReturn(response);

        var result = maintenanceManager.add(request);

        // Rule ordering matters: checkIfCarUnderMaintenance must run before ensureCarIsAvailable.
        InOrder ruleOrder = inOrder(rules);
        ruleOrder.verify(rules).checkIfCarUnderMaintenance(carId);
        ruleOrder.verify(rules).ensureCarIsAvailable(carId);

        var maintenanceCaptor = ArgumentCaptor.forClass(Maintenance.class);
        verify(repository).save(maintenanceCaptor.capture());
        var saved = maintenanceCaptor.getValue();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.isCompleted()).isFalse();
        assertThat(saved.getStartDate()).isNotNull();
        assertThat(saved.getEndDate()).isNull();

        var eventCaptor = ArgumentCaptor.forClass(MaintenanceCreatedEvent.class);
        verify(producer).sendMessage(eventCaptor.capture(), eq("maintenance-created"));
        assertThat(eventCaptor.getValue().getCarId()).isEqualTo(carId);

        assertThat(result).isSameAs(response);
    }

    @Test
    void update_whenMaintenanceExists_mapsAssignsIdAndSaves() {
        var id = UUID.randomUUID();
        var request = new UpdateMaintenanceRequest();
        var mappedMaintenance = new Maintenance();
        var response = new UpdateMaintenanceResponse();

        when(mapper.forRequest()).thenReturn(modelMapper);
        when(modelMapper.map(request, Maintenance.class)).thenReturn(mappedMaintenance);
        when(mapper.forResponse()).thenReturn(modelMapper);
        when(modelMapper.map(any(Maintenance.class), eq(UpdateMaintenanceResponse.class))).thenReturn(response);

        var result = maintenanceManager.update(id, request);

        verify(rules).checkIfMaintenanceExists(id);

        var maintenanceCaptor = ArgumentCaptor.forClass(Maintenance.class);
        verify(repository).save(maintenanceCaptor.capture());
        assertThat(maintenanceCaptor.getValue().getId()).isEqualTo(id);

        assertThat(result).isSameAs(response);
    }

    @Test
    void delete_publishesMaintenanceDeletedEventBeforeDeletingFromRepository() {
        var id = UUID.randomUUID();
        var carId = UUID.randomUUID();
        var maintenance = new Maintenance();
        maintenance.setCarId(carId);

        when(repository.findById(id)).thenReturn(Optional.of(maintenance));

        maintenanceManager.delete(id);

        verify(rules).checkIfMaintenanceExists(id);

        var eventCaptor = ArgumentCaptor.forClass(MaintenanceDeletedEvent.class);
        InOrder callOrder = inOrder(producer, repository);
        callOrder.verify(producer).sendMessage(eventCaptor.capture(), eq("maintenance-deleted"));
        callOrder.verify(repository).deleteById(id);

        assertThat(eventCaptor.getValue().getCarId()).isEqualTo(carId);
    }
}
