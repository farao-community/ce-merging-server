/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.global_grid_configurations.GridConfigurationRepository;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonBecConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonHvdcAlignmentConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonRegionConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.json.JsonXNodeConfiguration;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.AbstractGridConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.BECKeyConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.HvdcAlignmentConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.RegionConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.VirtualHubsConfigurationRecord;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.XNodeConfigurationRecord;
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
import static test_utils.CeTestUtils.S_IO_EXCEPTION;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

class ConfigurationServicesTest {
    private GridConfigurationRepository repository = mock(GridConfigurationRepository.class);

    private static final List<
        Pair<AbstractGridConfigurationService<? extends AbstractGridConfigurationRecord, ?>,
            Pair<? extends AbstractGridConfigurationRecord, Class<?>>>
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
    void shouldGetJsonConfig(final Pair<AbstractGridConfigurationService<? extends AbstractGridConfigurationRecord, ?>,
        Pair<? extends AbstractGridConfigurationRecord, Class<?>>> serviceAndConfigs) throws IOException {

        final AbstractGridConfigurationService<?, ?> service = serviceAndConfigs.getFirst();
        final AbstractGridConfigurationRecord recordObject = serviceAndConfigs.getSecond().getFirst();
        final Class<?> jsonConfigClass = serviceAndConfigs.getSecond().getSecond();
        service.setRepository(repository);

        when(repository.findFirstByValidFromLessThanEqualAndValidToGreaterThanOrderByPublishedOnDesc(
            any(LocalDateTime.class), any(LocalDateTime.class))
        ).thenReturn(recordObject);

        assertThat(service.getConfiguration(BEGINNING_OF_2000)).isInstanceOf(jsonConfigClass);
        assertThat(service.getConfigAsJsonBytes(BEGINNING_OF_2000)).isNotEmpty();
    }

    @ParameterizedTest
    @FieldSource("SERVICES_AND_CONFIGS")
    void shouldGetDefaultConfig(final Pair<AbstractGridConfigurationService<? extends AbstractGridConfigurationRecord, ?>,
        Pair<? extends AbstractGridConfigurationRecord, Class<?>>> serviceAndConfigs) throws IOException {

        final AbstractGridConfigurationService<?, ?> service = serviceAndConfigs.getFirst();
        final Class<?> jsonConfigClass = serviceAndConfigs.getSecond().getSecond();
        service.setRepository(repository);

        when(repository.findFirstByValidFromLessThanEqualAndValidToGreaterThanOrderByPublishedOnDesc(
            any(LocalDateTime.class), any(LocalDateTime.class))
        ).thenReturn(null);

        assertThat(service.getConfiguration(BEGINNING_OF_2000))
            .isInstanceOf(jsonConfigClass);
    }

    @ParameterizedTest
    @FieldSource("SERVICES_AND_CONFIGS")
    void shouldPublish(final Pair<AbstractGridConfigurationService<? extends AbstractGridConfigurationRecord, ?>,
        Pair<? extends AbstractGridConfigurationRecord, Class<?>>> serviceAndConfigs) throws IOException {

        final AbstractGridConfigurationService<?, ?> service = serviceAndConfigs.getFirst();
        final AbstractGridConfigurationRecord recordObject = serviceAndConfigs.getSecond().getFirst();
        service.setRepository(repository);

        when(repository.save(any())).thenReturn(recordObject);

        service.publish(new MockMultipartFile("testfile", service.getDefaultFileBytes()),
                        BEGINNING_OF_2000, BEGINNING_OF_2000);

        verify(repository).save(any());
    }

    @ParameterizedTest
    @FieldSource("SERVICES_AND_CONFIGS")
    void shouldThrowIfEmptyFile(final Pair<AbstractGridConfigurationService<? extends AbstractGridConfigurationRecord, ?>,
        Pair<? extends AbstractGridConfigurationRecord, Class<?>>> serviceAndConfigs) {

        final AbstractGridConfigurationService<?, ?> service = serviceAndConfigs.getFirst();
        assertThatThrownBy(() -> service.getTextContent(new MockMultipartFile("name", new byte[]{})))
            .isValidServiceException()
            .hasMessageContaining("empty");
    }

    @ParameterizedTest
    @FieldSource("SERVICES_AND_CONFIGS")
    void shouldThrowIfRepoErrorWhenPublishing(final Pair<AbstractGridConfigurationService<? extends AbstractGridConfigurationRecord, ?>,
        Pair<? extends AbstractGridConfigurationRecord, Class<?>>> serviceAndConfigs) {

        final AbstractGridConfigurationService<?, ?> service = serviceAndConfigs.getFirst();
        service.setRepository(repository);

        when(repository.save(any())).thenThrow(S_IO_EXCEPTION);

        assertThatThrownBy(() -> service.publish(new MockMultipartFile("testfile", service.getDefaultFileBytes()),
                                                 BEGINNING_OF_2000, BEGINNING_OF_2000))
            .isValidServiceException()
            .hasCause(S_IO_EXCEPTION)
            .hasMessageContaining("Configuration could not be published");
    }

}
