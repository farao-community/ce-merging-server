/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.global_grid_configurations.GridConfigurationRepository;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.records.AbstractGridConfigurationRecord;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static test_utils.CeTestUtils.BEGINNING_OF_2000;
import static test_utils.CeTestUtils.S_IO_EXCEPTION;
import static test_utils.assertions.CeThrowableAssert.assertThatThrownBy;

final class ConfigurationServicesTestHelper<T extends AbstractGridConfigurationRecord> {

    final AbstractGridConfigurationService<T, ?> service;
    final T recordObject;
    final Class<?> jsonConfigClass;
    final GridConfigurationRepository<T> repo;

    public ConfigurationServicesTestHelper(final AbstractGridConfigurationService<T, ?> service,
                                           final T recordObject,
                                           final Class<?> jsonConfigClass) {
        this.service = service;
        this.recordObject = recordObject;
        this.jsonConfigClass = jsonConfigClass;
        this.repo = service.getRepository();
    }

    public void testAllAbstractMethods() throws IOException {
        shouldGetDefaultConfig();
        shouldGetJsonConfig();
        shouldPublish();
        shouldThrowIfEmptyFile();
        shouldThrowIfRepoErrorWhenPublishing();
    }

    public void testAllExceptPublish() throws IOException {
        shouldGetDefaultConfig();
        shouldGetJsonConfig();
        shouldThrowIfEmptyFile();
        shouldThrowIfRepoErrorWhenPublishing();
    }

    private void shouldGetJsonConfig() throws IOException {

        when(repo.findFirstByValidFromLessThanEqualAndValidToGreaterThanOrderByPublishedOnDesc(
            any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(recordObject);

        assertThat(service.getConfiguration(BEGINNING_OF_2000)).isInstanceOf(jsonConfigClass);
        assertThat(service.getConfigAsJsonBytes(BEGINNING_OF_2000)).isNotEmpty();
    }

    private void shouldGetDefaultConfig() throws IOException {

        when(repo.findFirstByValidFromLessThanEqualAndValidToGreaterThanOrderByPublishedOnDesc(
            any(LocalDateTime.class), any(LocalDateTime.class))
        ).thenReturn(null);

        assertThat(service.getConfiguration(BEGINNING_OF_2000))
            .isInstanceOf(jsonConfigClass);
    }

    private void shouldPublish() throws IOException {

        when(repo.save(any())).thenReturn(recordObject);

        service.publish(new MockMultipartFile("testfile", service.getDefaultFileBytes()),
                        BEGINNING_OF_2000, BEGINNING_OF_2000);

        verify(repo).save(any());
    }

    private void shouldThrowIfEmptyFile() {
        assertThatThrownBy(() -> service.getTextContent(new MockMultipartFile("name", new byte[]{})))
            .isValidServiceException()
            .hasMessageContaining("empty");
    }

    private void shouldThrowIfRepoErrorWhenPublishing() {
        when(repo.save(any())).thenThrow(S_IO_EXCEPTION);

        assertThatThrownBy(() -> service.publish(new MockMultipartFile("testfile", service.getDefaultFileBytes()),
                                                 BEGINNING_OF_2000, BEGINNING_OF_2000))
            .isValidServiceException()
            .hasCause(S_IO_EXCEPTION)
            .hasMessageContaining("Configuration could not be published");
    }

}
