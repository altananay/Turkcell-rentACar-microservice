package com.kodlamaio.inventoryservice.business.rules;

import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.inventoryservice.repository.ModelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelBusinessRulesTest {

    @Mock private ModelRepository repository;

    @InjectMocks
    private ModelBusinessRules rules;

    @Test
    void checkIfModelExists_whenModelExists_doesNotThrow() {
        var modelId = UUID.randomUUID();
        when(repository.existsById(modelId)).thenReturn(true);

        rules.checkIfModelExists(modelId);
    }

    @Test
    void checkIfModelExists_whenModelMissing_throwsBusinessException() {
        var modelId = UUID.randomUUID();
        when(repository.existsById(modelId)).thenReturn(false);

        assertThatThrownBy(() -> rules.checkIfModelExists(modelId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("MODEL_NOT_EXISTS");
    }
}
