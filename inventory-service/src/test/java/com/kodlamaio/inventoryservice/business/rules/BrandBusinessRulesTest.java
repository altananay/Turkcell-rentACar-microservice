package com.kodlamaio.inventoryservice.business.rules;

import com.kodlamaio.commonpackage.utils.exceptions.BusinessException;
import com.kodlamaio.inventoryservice.repository.BrandRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandBusinessRulesTest {

    @Mock private BrandRepository repository;

    @InjectMocks
    private BrandBusinessRules rules;

    @Test
    void checkIfBrandExists_whenBrandExists_doesNotThrow() {
        var brandId = UUID.randomUUID();
        when(repository.existsById(brandId)).thenReturn(true);

        rules.checkIfBrandExists(brandId);
    }

    @Test
    void checkIfBrandExists_whenBrandMissing_throwsBusinessException() {
        var brandId = UUID.randomUUID();
        when(repository.existsById(brandId)).thenReturn(false);

        assertThatThrownBy(() -> rules.checkIfBrandExists(brandId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("BRAND_NOT_EXISTS");
    }
}
