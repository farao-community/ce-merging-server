package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.global_grid_configurations.GridConfigurationRepository;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonXNodeConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.XNodeConfigurationRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.mock;

class XNodeConfigurationServiceTest {
    final GridConfigurationRepository<XNodeConfigurationRecord> repository = mock(GridConfigurationRepository.class);
    final XNodeConfigurationService service = new XNodeConfigurationService(repository);

    @Test
    void shouldHaveCommonMethodsWorking() throws IOException {
        new ConfigurationServicesTestHelper<>(service,
                                              new XNodeConfigurationRecord(),
                                              JsonXNodeConfiguration.class)
            .testAllAbstractMethods();
    }
}
