/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.global_grid_configurations.model.AbstractGridConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.bilateral_exchanges.BECKeyConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.bilateral_exchanges.JsonBecConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.hvdc_alignment.HvdcAlignmentConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.hvdc_alignment.JsonHvdcAlignmentConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.region_eic.JsonRegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.region_eic.RegionConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.virtual_hubs.VirtualHubsConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.xnodes.JsonXNodeConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.xnodes.XNodeConfigurationRecord;
import com.powsybl.openrao.virtualhubs.VirtualHubsConfiguration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.springframework.data.util.Pair;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.BEGINNING_OF_2000;

class GridConfigurationServicesTest {
    private GridConfigurationRepository repository = mock(GridConfigurationRepository.class);

    private static final List<
        Pair<AbstractGridConfigurationService<? extends AbstractGridConfiguration, ?>,
            Pair<? extends AbstractGridConfiguration, Class<?>>>
        > SERVICES_AND_CONFIGS = List.of(Pair.of(new BECKeyConfigurationService(new RegionConfigurationService()),
                                                 Pair.of(new BECKeyConfigurationRecord(),
                                                         JsonBecConfiguration.class)),
                                         Pair.of(new HvdcAlignmentConfigurationService(new VirtualHubsConfigurationService()),
                                                 Pair.of(new HvdcAlignmentConfigurationRecord(),
                                                         JsonHvdcAlignmentConfiguration.class)),
                                         Pair.of(new RegionConfigurationService(),
                                                 Pair.of(new RegionConfigurationRecord(),
                                                         JsonRegionConfiguration.class)),
                                         Pair.of(new VirtualHubsConfigurationService(),
                                                 Pair.of(new VirtualHubsConfigurationRecord(),
                                                         VirtualHubsConfiguration.class)),
                                         Pair.of(new XNodeConfigurationService(),
                                                 Pair.of(new XNodeConfigurationRecord(),
                                                         JsonXNodeConfiguration.class)));

    @ParameterizedTest
    @FieldSource("SERVICES_AND_CONFIGS")
    void shouldGetDefaultJsonConfig(final Pair<AbstractGridConfigurationService<? extends AbstractGridConfiguration, ?>,
        Pair<? extends AbstractGridConfiguration, Class<?>>> serviceAndConfigs) throws IOException {
        final AbstractGridConfigurationService<?, ?> service = serviceAndConfigs.getFirst();
        service.setRepository(repository);
        when(repository.findLastPublishedValid(any(LocalDateTime.class)))
            .thenReturn(serviceAndConfigs.getSecond().getFirst());

        assertThat(service.getConfiguration(BEGINNING_OF_2000))
            .isInstanceOf(serviceAndConfigs.getSecond().getSecond());
    }

    @ParameterizedTest
    @FieldSource("SERVICES_AND_CONFIGS")
    void shouldPublish(final Pair<AbstractGridConfigurationService<? extends AbstractGridConfiguration, ?>,
        Pair<? extends AbstractGridConfiguration, Class<?>>> serviceAndConfigs) throws IOException {
        final AbstractGridConfigurationService<?, ?> service = serviceAndConfigs.getFirst();
        service.setRepository(repository);

        when(repository.save(any()))
            .thenReturn(serviceAndConfigs.getSecond().getFirst());

        service.publish(new MockMultipartFile("testfile", service.getDefaultFileBytes()),
                        BEGINNING_OF_2000, BEGINNING_OF_2000);

        verify(repository).save(any());
    }

}
