/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.services;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.JsonUtils;
import com.farao_community.farao.ce_merging.global_grid_configurations.model.GridConfigurationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.time.OffsetDateTime;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 * @param <R> as in Record
 * @param <C> as in (JSON) Configuration
 */
public abstract class AbstractGridConfigurationService<R extends GridConfigurationRecord, C> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGridConfigurationService.class);
    private static final String DEFAULT_CONFIGURATIONS_DIR = "gridDefaultConfigurations/%s";

    protected GridConfigurationRepository<R> repository;

    protected abstract C getDefaultJsonConfiguration(final OffsetDateTime targetDate) throws IOException;

    protected abstract C getJsonConfigurationFromRecord(final R cfgRecord);

    protected abstract R getConfigurationRecordFromFile(final MultipartFile configurationFile,
                                                        final OffsetDateTime validFrom,
                                                        final OffsetDateTime validTo) throws IOException;

    protected String generateUuidString() {
        return UUID.randomUUID().toString();
    }

    @SuppressWarnings("unchecked")
    private Class<C> getJsonConfigClass() {
        return (Class<C>) ((ParameterizedType) getClass()
            .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    protected InputStream getDefaultConfigFileStream() throws IOException {
        final String configFileName = DefaultConfigFileNameFactory.getDefaultConfigFileName(getJsonConfigClass());
        return new ClassPathResource(DEFAULT_CONFIGURATIONS_DIR.formatted(configFileName)).getInputStream();
    }

    protected C readFromJson(final InputStream filePath) {
        return JsonUtils.read(getJsonConfigClass(), filePath);
    }

    protected C readFromJson(final String fileContent) throws FileNotFoundException {
        return JsonUtils.read(getJsonConfigClass(), fileContent);
    }

    protected byte[] getDefaultFileBytes() throws IOException {
        return getDefaultConfigFileStream().readAllBytes();
    }

    protected void publish(final MultipartFile configurationFile,
                        final OffsetDateTime validFrom,
                        final OffsetDateTime validTo) {
        try {
            repository.save(getConfigurationRecordFromFile(configurationFile, validFrom, validTo));
        } catch (final Exception e) {
            LOGGER.error("Configuration cannot be published to server");
            throw new CeMergingException("Configuration could not be published, file or dates could be invalid.", e);
        }
    }

    protected byte[] getConfigurationAsBytes(final OffsetDateTime targetDate) throws IOException {
        return JsonUtils.writeToBytes(getJsonConfigClass(), getConfiguration(targetDate));
    }

    public C getConfiguration(final OffsetDateTime targetDate) throws IOException {
        try {
            final R configRecord = repository.findLastPublishedValid(targetDate.toLocalDateTime());
            LOGGER.info("configuration retrieved from server");
            return getJsonConfigurationFromRecord(configRecord);
        } catch (final Exception e) {
            LOGGER.warn("configuration cannot be retrieved, default configuration will be used");
            return getDefaultJsonConfiguration(targetDate);
        }
    }

    protected String getTextContent(final MultipartFile file) throws IOException {
        final String fileContent = new String(file.getBytes(), UTF_8);
        if (fileContent.isEmpty()) {
            throw new CeMergingException("file %s is empty".formatted(file.getOriginalFilename()));
        }
        return fileContent;
    }

    public GridConfigurationRepository<R> getRepository() {
        return repository;
    }

    public void setRepository(final GridConfigurationRepository<R> repository) {
        this.repository = repository;
    }

}
