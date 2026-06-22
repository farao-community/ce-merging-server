package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.global_grid_configurations.GridConfigurationRepository;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.VirtualHubsConfigurationRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.BEGINNING_OF_2000;

class VirtualHubsConfigurationServiceTest {

    final GridConfigurationRepository<VirtualHubsConfigurationRecord> repository = mock(GridConfigurationRepository.class);
    final VirtualHubsConfigurationService service = new VirtualHubsConfigurationService();

    @Test
    void shouldGetJsonConfigCallingRecord() throws IOException {

        service.setRepository(repository);
        final VirtualHubsConfigurationRecord mock = mock(VirtualHubsConfigurationRecord.class);

        when(repository.findFirstByValidFromLessThanEqualAndValidToGreaterThanOrderByPublishedOnDesc(
            any(LocalDateTime.class), any(LocalDateTime.class))
        ).thenReturn(mock);

        service.getConfiguration(BEGINNING_OF_2000);

        verify(mock).getConfigurationJson();
    }

}