/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util.serialization;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

@NoArgsConstructor(access = AccessLevel.NONE)
@Slf4j
public final class JaxbUtils {
    public static <T> T read(final Class<T> clazz,
                             final String path) {
        try (final InputStream fileContent = Files.newInputStream(Paths.get(path))) {
            return JAXBContext
                .newInstance(clazz)
                .createUnmarshaller()
                .unmarshal(new StreamSource(fileContent), clazz)
                .getValue();
        } catch (final JAXBException | IOException e) {
            final String errorMessage = String.format("Error occurred when converting xml file %s to object of type %s",
                                                      path, clazz.getName());
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

}
