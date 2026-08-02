package com.kodlamaio.commonpackage.utils.mappers;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import static org.assertj.core.api.Assertions.assertThat;

class ModelMapperManagerTest {

    private final ModelMapper mapper = new ModelMapper();
    private final ModelMapperManager manager = new ModelMapperManager(mapper);

    @Test
    void forResponse_setsLooseMatchingAndIgnoresAmbiguity_andReturnsSameMapperInstance() {
        var result = manager.forResponse();

        assertThat(result).isSameAs(mapper);
        assertThat(mapper.getConfiguration().getMatchingStrategy()).isEqualTo(MatchingStrategies.LOOSE);
        assertThat(mapper.getConfiguration().isAmbiguityIgnored()).isTrue();
    }

    @Test
    void forRequest_setsStandardMatchingAndIgnoresAmbiguity_andReturnsSameMapperInstance() {
        var result = manager.forRequest();

        assertThat(result).isSameAs(mapper);
        assertThat(mapper.getConfiguration().getMatchingStrategy()).isEqualTo(MatchingStrategies.STANDARD);
        assertThat(mapper.getConfiguration().isAmbiguityIgnored()).isTrue();
    }
}
