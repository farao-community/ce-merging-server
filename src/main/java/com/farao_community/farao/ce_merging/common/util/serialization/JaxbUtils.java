/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.ce_merging.common.util.serialization;

import com.farao_community.farao.ce_merging.common.exception.ServiceIOException;
import com.google.common.io.ByteSource;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static java.lang.Boolean.TRUE;

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

    public static <T> T readBytes(final Class<T> clazz,
                                  final byte[] fileContent) {
        try {
            return (T) JAXBContext
                .newInstance(clazz)
                .createUnmarshaller()
                .unmarshal(ByteSource.wrap(fileContent).openStream());
        } catch (final JAXBException | IOException e) {
            final String errorMessage = String.format("Error occurred when converting bytes to object of type %s", clazz.getName());
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    public static <T> byte[] writeInBytes(final Class<T> clazz, final T type) {
        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            marshallerFormatOutput(clazz).marshal(type, bos);
            return bos.toByteArray();
        } catch (final JAXBException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s to bytes", clazz.getName());
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    public static <T> void writeInPath(final Class<T> clazz,
                                       final T type,
                                       final Path filePath) {
        try {
            marshallerFormatOutput(clazz).marshal(type, filePath.toFile());
        } catch (JAXBException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s to path %s", clazz.getName(), filePath.toString());
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    // Solution to marshal an object without @XmlRootElement annotation
    public static <T> byte[] writeInBytes(final Class<T> clazz,
                                          final T type,
                                          final String nameSpaceURI,
                                          final String rootElement) {
        try {
            final JAXBElement<T> jaxbElement =
                new JAXBElement<>(new QName(nameSpaceURI, rootElement),
                                   clazz,
                                   type);
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            marshallerFormatOutput(clazz).marshal(jaxbElement, bos);
            return bos.toByteArray();
        } catch (final JAXBException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s to bytes",
                                                      clazz.getName());
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    // Solution to marshal a object without @XmlRootElement annotation
    public static <T> void writeInPath(Class<T> clazz, T type, String nameSpaceURI, String rootElement, Path filePath) {
        try {
            final JAXBElement<T> jaxbElement =
                new JAXBElement<>(new QName(nameSpaceURI, rootElement),
                                   clazz,
                                   type);
            marshallerFormatOutput(clazz).marshal(jaxbElement, filePath.toFile());
        } catch (final JAXBException e) {
            final String errorMessage = String.format("Error occurred when writing content of object of type %s to path %s",
                                                      clazz.getName(), filePath.toString());
            log.error(errorMessage);
            throw new ServiceIOException(errorMessage, e);
        }
    }

    private static <T> Marshaller marshallerFormatOutput(final Class<T> clazz) throws JAXBException {
        final Marshaller jaxbMarshaller = JAXBContext.newInstance(clazz).createMarshaller();
        jaxbMarshaller.setProperty(JAXB_FORMATTED_OUTPUT, TRUE);
        return jaxbMarshaller;
    }

}
