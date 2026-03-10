/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.global_grid_configurations.abstraction;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.common.util.serialization.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.farao_community.farao.ce_merging.common.CeMergingConstants.FILE_IS_EMPTY;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 *
 * @param <R> as in Record
 * @param <C> as in (JSON) Configuration
 */
@Slf4j
public abstract class AbstractGridConfigurationService<R, C> {

    private static final String DEFAULT_CONFIGURATIONS_DIR = "gridDefaultConfigurations/%s";

    protected GridConfigurationRepository<R> repository;

    protected abstract C getDefaultConfiguration(final OffsetDateTime targetDate) throws IOException;

    protected abstract C getConfigurationfromRecord(final R cfgRecord);

    protected abstract R buildFromFile(final MultipartFile configurationFile,
                                       final OffsetDateTime validFrom,
                                       final OffsetDateTime validTo) throws IOException;

    protected String generateId() {
        return UUID.randomUUID().toString();
    }

    @SuppressWarnings("unchecked")
    private Class<C> getConfigObjectClass() {
        return (Class<C>) ((ParameterizedType) getClass()
            .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    protected InputStream getDefaultFileStream() throws IOException {
        final String configFileName = DefaultConfigFileNameFactory.getDefaultConfigFileName(getConfigObjectClass());
        return new ClassPathResource(DEFAULT_CONFIGURATIONS_DIR.formatted(configFileName)).getInputStream();
    }

    protected byte[] getDefaultFileBytes() throws IOException {
        return getDefaultFileStream().readAllBytes();
    }

    protected void publish(final MultipartFile configurationFile,
                        final OffsetDateTime validFrom,
                        final OffsetDateTime validTo) {
        try {
            repository.save(buildFromFile(configurationFile, validFrom, validTo));
        } catch (final Exception e) {
            log.error("Configuration cannot be published to server");
            throw new CeMergingException("Configuration could not be published, file or dates could be invalid.", e);
        }
    }

    protected byte[] getConfigurationAsBytes(final OffsetDateTime targetDate) throws IOException {
        return JsonUtils.writeInBytes(getConfigObjectClass(), getConfiguration(targetDate));
    }

    public C getConfiguration(final OffsetDateTime targetDate) throws IOException {
        try {
            final R cfg = repository.findLastPublishedValid(targetDate.toLocalDateTime());
            log.info("configuration retrieved from server");
            return getConfigurationfromRecord(cfg);
        } catch (final Exception e) {
            log.warn("configuration cannot be retrieved, default configuration will be used");
            return getDefaultConfiguration(targetDate);
        }
    }

    protected String getTextContent(final MultipartFile file) throws IOException {
        final String fileContent = new String(file.getBytes(), UTF_8);
        if (fileContent.isEmpty()) {
            throw new CeMergingException(FILE_IS_EMPTY);
        }
        return fileContent;
    }

}
