/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.merging.process;

import com.farao_community.farao.ce_merging.common.exception.CeMergingException;
import com.farao_community.farao.ce_merging.merging.task.entities.SavedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class FileStorageUtils {

    private FileStorageUtils() {
        // utility
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageUtils.class);

    public static SavedFile save(final String directory, final String fileName, final String location, final ThrowingConsumer<Path> writer) {
        final Path filePath = Paths.get(directory, fileName);
        try {
            writer.accept(filePath);
            LOGGER.info("File '{}' has been saved", fileName);
            return new SavedFile(fileName, filePath.toString(), location);
        } catch (final Exception e) {
            LOGGER.error("Cannot write file '{}'", fileName, e);
            throw new CeMergingException(String.format("Cannot write file '%s'", fileName), e);
        }
    }

    @FunctionalInterface
    public interface ThrowingConsumer<T> {
        void accept(T t) throws Exception;
    }
}
